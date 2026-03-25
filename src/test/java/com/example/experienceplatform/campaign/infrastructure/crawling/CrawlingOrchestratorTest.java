package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.application.crawling.CrawlingOrchestrator;
import com.example.experienceplatform.campaign.domain.*;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogRepository;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlingOrchestratorTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CrawlingSourceRepository crawlingSourceRepository;

    @Mock
    private CrawlingLogRepository crawlingLogRepository;

    @Mock
    private CrawlingProperties crawlingProperties;

    @Mock
    private AddressRegionMatcher addressRegionMatcher;

    @Mock
    private CampaignCrawler mockCrawler;

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);
    private static final CrawlingSource GANGNAM_SOURCE =
            new CrawlingSource("GANGNAM", "강남맛집", "https://www.gangnam.kr", null, null, "GANGNAM", 2);

    @BeforeEach
    void setUp() {
        when(crawlingProperties.getParallelThreads()).thenReturn(5);
        lenient().when(addressRegionMatcher.match(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("전체 실행 - 성공")
    void executeAll_success() {
        CrawledCampaign item = createCrawledCampaign("test-1", "테스트 캠페인");
        when(mockCrawler.getCrawlerType()).thenReturn("REVU");
        when(mockCrawler.crawl(REVU_SOURCE)).thenReturn(List.of(item));
        when(crawlingSourceRepository.findAllActiveOrderByDisplayOrder()).thenReturn(List.of(REVU_SOURCE));
        when(campaignRepository.findByCrawlingSourceAndOriginalId(any(), any())).thenReturn(Optional.empty());
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.findExpiredRecruitingCampaigns(any())).thenReturn(List.of());
        when(crawlingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CrawlerRegistry registry = new CrawlerRegistry(List.of(mockCrawler));
        CrawlingOrchestrator testOrchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository, crawlingProperties, addressRegionMatcher);
        testOrchestrator.initExecutor();

        List<CrawlingResult> results = testOrchestrator.executeAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNewCount()).isEqualTo(1);
        verify(crawlingLogRepository).save(any(CrawlingLog.class));
    }

    @Test
    @DisplayName("기존 데이터 update")
    void executeAll_updateExisting() {
        CrawledCampaign item = createCrawledCampaign("existing-1", "업데이트 캠페인");
        Campaign existing = new Campaign(
                REVU_SOURCE, "existing-1", "기존", null, null, "http://test.com",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING, 5, null, null, null);

        when(mockCrawler.getCrawlerType()).thenReturn("REVU");
        when(mockCrawler.crawl(REVU_SOURCE)).thenReturn(List.of(item));
        when(crawlingSourceRepository.findAllActiveOrderByDisplayOrder()).thenReturn(List.of(REVU_SOURCE));
        when(campaignRepository.findByCrawlingSourceAndOriginalId(REVU_SOURCE, "existing-1"))
                .thenReturn(Optional.of(existing));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.findExpiredRecruitingCampaigns(any())).thenReturn(List.of());
        when(crawlingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CrawlerRegistry registry = new CrawlerRegistry(List.of(mockCrawler));
        CrawlingOrchestrator testOrchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository, crawlingProperties, addressRegionMatcher);
        testOrchestrator.initExecutor();

        List<CrawlingResult> results = testOrchestrator.executeAll();

        assertThat(results.get(0).getUpdatedCount()).isEqualTo(1);
        assertThat(results.get(0).getNewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("크롤러 하나 실패 시 나머지 계속")
    void executeAll_partialFailure() {
        CampaignCrawler failCrawler = mock(CampaignCrawler.class);
        CampaignCrawler successCrawler = mock(CampaignCrawler.class);

        when(failCrawler.getCrawlerType()).thenReturn("REVU");
        when(failCrawler.crawl(REVU_SOURCE)).thenThrow(new RuntimeException("실패"));
        when(successCrawler.getCrawlerType()).thenReturn("GANGNAM");
        when(successCrawler.crawl(GANGNAM_SOURCE)).thenReturn(List.of());
        when(crawlingSourceRepository.findAllActiveOrderByDisplayOrder()).thenReturn(List.of(REVU_SOURCE, GANGNAM_SOURCE));
        when(campaignRepository.findExpiredRecruitingCampaigns(any())).thenReturn(List.of());
        when(crawlingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CrawlerRegistry registry = new CrawlerRegistry(List.of(failCrawler, successCrawler));
        CrawlingOrchestrator testOrchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository, crawlingProperties, addressRegionMatcher);
        testOrchestrator.initExecutor();

        List<CrawlingResult> results = testOrchestrator.executeAll();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getStatus()).isEqualTo(CrawlingLogStatus.FAILED);
        assertThat(results.get(1).getStatus()).isEqualTo(CrawlingLogStatus.SUCCESS);
    }

    private CrawledCampaign createCrawledCampaign(String originalId, String title) {
        return new CrawledCampaign(
                "REVU", originalId, title, "설명", null, null,
                "http://test.com/" + originalId, CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, LocalDate.now().plusDays(10), null, null, null, null, null
        );
    }
}
