package com.example.experienceplatform.campaign.application.crawling;

import com.example.experienceplatform.campaign.domain.CampaignRepository;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.domain.CrawlingSourceRepository;
import com.example.experienceplatform.campaign.domain.exception.CrawlerNotFoundException;
import com.example.experienceplatform.campaign.domain.exception.CrawlingSourceNotFoundException;
import com.example.experienceplatform.campaign.domain.exception.DuplicateSourceCodeException;
import com.example.experienceplatform.campaign.infrastructure.crawling.CampaignCrawler;
import com.example.experienceplatform.campaign.infrastructure.crawling.CrawledCampaign;
import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlerRegistry;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CrawlingSourceService {

    private final CrawlingSourceRepository crawlingSourceRepository;
    private final CampaignRepository campaignRepository;
    private final CrawlingLogRepository crawlingLogRepository;
    private final CrawlerRegistry crawlerRegistry;

    public CrawlingSourceService(CrawlingSourceRepository crawlingSourceRepository,
                                 CampaignRepository campaignRepository,
                                 CrawlingLogRepository crawlingLogRepository,
                                 CrawlerRegistry crawlerRegistry) {
        this.crawlingSourceRepository = crawlingSourceRepository;
        this.campaignRepository = campaignRepository;
        this.crawlingLogRepository = crawlingLogRepository;
        this.crawlerRegistry = crawlerRegistry;
    }

    public List<CrawlingSourceInfo> getAllSources() {
        List<CrawlingSource> sources = crawlingSourceRepository.findAllOrderByDisplayOrder();
        return sources.stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional
    public CrawlingSourceInfo create(CrawlingSourceCreateCommand command) {
        if (crawlingSourceRepository.existsByCode(command.getCode())) {
            throw new DuplicateSourceCodeException(command.getCode());
        }

        CrawlingSource source = new CrawlingSource(
                command.getCode(), command.getName(), command.getBaseUrl(),
                command.getListUrlPattern(), command.getDescription(),
                command.getCrawlerType(), command.getDisplayOrder());

        CrawlingSource saved = crawlingSourceRepository.save(source);
        return toInfo(saved);
    }

    @Transactional
    public CrawlingSourceInfo update(Long id, CrawlingSourceUpdateCommand command) {
        CrawlingSource source = crawlingSourceRepository.findById(id)
                .orElseThrow(() -> new CrawlingSourceNotFoundException(id));

        source.update(command.getName(), command.getBaseUrl(), command.getListUrlPattern(),
                command.getDescription(), command.getCrawlerType(), command.getDisplayOrder());

        CrawlingSource saved = crawlingSourceRepository.save(source);
        return toInfo(saved);
    }

    @Transactional
    public CrawlingSourceInfo toggleActive(Long id) {
        CrawlingSource source = crawlingSourceRepository.findById(id)
                .orElseThrow(() -> new CrawlingSourceNotFoundException(id));

        if (source.isActive()) {
            source.deactivate();
        } else {
            source.activate();
        }

        CrawlingSource saved = crawlingSourceRepository.save(source);
        return toInfo(saved);
    }

    public CrawlingTestResult testCrawl(Long id) {
        CrawlingSource source = crawlingSourceRepository.findById(id)
                .orElseThrow(() -> new CrawlingSourceNotFoundException(id));

        CampaignCrawler crawler = crawlerRegistry.findByCrawlerType(source.getCrawlerType())
                .orElseThrow(() -> new CrawlerNotFoundException(source.getCrawlerType()));

        try {
            List<CrawledCampaign> items = crawler.crawl(source);
            return CrawlingTestResult.success(source.getCode(), source.getName(),
                    source.getCrawlerType(), items);
        } catch (Exception e) {
            return CrawlingTestResult.failed(source.getCode(), source.getName(),
                    source.getCrawlerType(), e.getMessage());
        }
    }

    public List<String> getAvailableCrawlerTypes() {
        return crawlerRegistry.getAvailableCrawlerTypes();
    }

    private CrawlingSourceInfo toInfo(CrawlingSource source) {
        long campaignCount = campaignRepository.countByCrawlingSource(source);
        LocalDateTime lastCrawledAt = crawlingLogRepository
                .findLatestBySourceCode(source.getCode())
                .map(CrawlingLog::getExecutedAt)
                .orElse(null);
        return new CrawlingSourceInfo(source, campaignCount, lastCrawledAt);
    }
}
