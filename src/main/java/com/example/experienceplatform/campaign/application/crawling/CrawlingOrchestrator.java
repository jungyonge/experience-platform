package com.example.experienceplatform.campaign.application.crawling;

import com.example.experienceplatform.campaign.domain.*;
import com.example.experienceplatform.campaign.domain.exception.CrawlingSourceNotFoundException;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogRepository;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CrawlingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CrawlingOrchestrator.class);

    private final CrawlerRegistry crawlerRegistry;
    private final CampaignRepository campaignRepository;
    private final CrawlingSourceRepository crawlingSourceRepository;
    private final CrawlingLogRepository crawlingLogRepository;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public CrawlingOrchestrator(CrawlerRegistry crawlerRegistry,
                                CampaignRepository campaignRepository,
                                CrawlingSourceRepository crawlingSourceRepository,
                                CrawlingLogRepository crawlingLogRepository) {
        this.crawlerRegistry = crawlerRegistry;
        this.campaignRepository = campaignRepository;
        this.crawlingSourceRepository = crawlingSourceRepository;
        this.crawlingLogRepository = crawlingLogRepository;
    }

    public List<CrawlingResult> executeAll() {
        if (!isRunning.compareAndSet(false, true)) {
            throw new CrawlingInProgressException();
        }
        try {
            List<CrawlingSource> activeSources = crawlingSourceRepository.findAllActiveOrderByDisplayOrder();
            List<CrawlingResult> results = new ArrayList<>();
            for (CrawlingSource source : activeSources) {
                Optional<CampaignCrawler> crawlerOpt = crawlerRegistry.findByCrawlerType(source.getCrawlerType());
                if (crawlerOpt.isEmpty()) {
                    log.warn("매칭되는 크롤러가 없습니다: sourceCode={}, crawlerType={}",
                            source.getCode(), source.getCrawlerType());
                    CrawlingResult failedResult = CrawlingResult.failed(
                            source.getCode(), source.getName(),
                            "매칭되는 크롤러가 없습니다: " + source.getCrawlerType(), 0);
                    saveLog(failedResult, source.getCrawlerType());
                    results.add(failedResult);
                    continue;
                }
                CrawlingResult result = executeCrawler(crawlerOpt.get(), source);
                results.add(result);
            }
            closeExpiredCampaigns(LocalDate.now());
            return results;
        } finally {
            isRunning.set(false);
        }
    }

    public CrawlingResult executeBySourceCode(String sourceCode) {
        if (!isRunning.compareAndSet(false, true)) {
            throw new CrawlingInProgressException();
        }
        try {
            CrawlingSource source = crawlingSourceRepository.findByCode(sourceCode)
                    .orElseThrow(() -> new CrawlingSourceNotFoundException(sourceCode));

            CampaignCrawler crawler = crawlerRegistry.findByCrawlerType(source.getCrawlerType())
                    .orElseThrow(() -> new CrawlingException(
                            "크롤러를 찾을 수 없습니다: crawlerType=" + source.getCrawlerType()));

            CrawlingResult result = executeCrawler(crawler, source);
            closeExpiredCampaigns(LocalDate.now());
            return result;
        } finally {
            isRunning.set(false);
        }
    }

    private CrawlingResult executeCrawler(CampaignCrawler crawler, CrawlingSource source) {
        long startTime = System.currentTimeMillis();
        String sourceCode = source.getCode();
        String sourceName = source.getName();

        try {
            List<CrawledCampaign> crawled = crawler.crawl(source);
            int newCount = 0;
            int updatedCount = 0;
            int failedCount = 0;

            for (CrawledCampaign item : crawled) {
                try {
                    boolean isNew = upsert(item, source);
                    if (isNew) newCount++;
                    else updatedCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.warn("아이템 upsert 실패 [{}/{}]: {}", sourceCode, item.getOriginalId(), e.getMessage());
                }
            }

            long durationMs = System.currentTimeMillis() - startTime;
            CrawlingResult result = CrawlingResult.success(sourceCode, sourceName, crawled.size(), newCount, updatedCount, failedCount, durationMs);
            saveLog(result, source.getCrawlerType());
            return result;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("{} 크롤링 실패: {}", sourceCode, e.getMessage());
            CrawlingResult result = CrawlingResult.failed(sourceCode, sourceName, e.getMessage(), durationMs);
            saveLog(result, source.getCrawlerType());
            return result;
        }
    }

    @Transactional
    protected boolean upsert(CrawledCampaign item, CrawlingSource source) {
        var existing = campaignRepository.findByCrawlingSourceAndOriginalId(source, item.getOriginalId());

        if (existing.isPresent()) {
            Campaign campaign = existing.get();
            campaign.update(
                    item.getTitle(), item.getDescription(), item.getThumbnailUrl(),
                    item.getOriginalUrl(), item.getCategory(), item.getStatus(),
                    item.getRecruitCount(), item.getApplyStartDate(), item.getApplyEndDate(),
                    item.getAnnouncementDate(), item.getDetailContent(), item.getReward(),
                    item.getMission(), item.getAddress(), item.getKeywords());
            campaignRepository.save(campaign);
            return false;
        } else {
            Campaign campaign = new Campaign(
                    source, item.getOriginalId(), item.getTitle(),
                    item.getDescription(), item.getThumbnailUrl(), item.getOriginalUrl(),
                    item.getCategory(), item.getStatus(), item.getRecruitCount(),
                    item.getApplyStartDate(), item.getApplyEndDate(), item.getAnnouncementDate(),
                    item.getDetailContent(), item.getReward(), item.getMission(),
                    item.getAddress(), item.getKeywords());
            try {
                campaignRepository.save(campaign);
            } catch (DataIntegrityViolationException e) {
                log.warn("중복 캠페인 skip: {}/{}", item.getSourceCode(), item.getOriginalId());
                return false;
            }
            return true;
        }
    }

    @Transactional
    protected void closeExpiredCampaigns(LocalDate today) {
        List<Campaign> expired = campaignRepository.findExpiredRecruitingCampaigns(today);
        for (Campaign campaign : expired) {
            campaign.update(
                    campaign.getTitle(), campaign.getDescription(), campaign.getThumbnailUrl(),
                    campaign.getOriginalUrl(), campaign.getCategory(), CampaignStatus.CLOSED,
                    campaign.getRecruitCount(), campaign.getApplyStartDate(), campaign.getApplyEndDate(),
                    campaign.getAnnouncementDate(), campaign.getDetailContent(), campaign.getReward(),
                    campaign.getMission(), campaign.getAddress(), campaign.getKeywords());
            campaignRepository.save(campaign);
        }
        if (!expired.isEmpty()) {
            log.info("만료 캠페인 {}건 CLOSED 처리", expired.size());
        }
    }

    private void saveLog(CrawlingResult result, String crawlerType) {
        CrawlingLog crawlingLog = new CrawlingLog(
                result.getSourceCode(), result.getSourceName(), crawlerType,
                result.getStatus(), result.getTotalCrawled(), result.getNewCount(),
                result.getUpdatedCount(), result.getFailedCount(),
                result.getErrorMessage(), result.getDurationMs());
        crawlingLogRepository.save(crawlingLog);
    }
}
