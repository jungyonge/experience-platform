package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.application.crawling.CrawlingOrchestrator;
import com.example.experienceplatform.campaign.domain.*;
import com.example.experienceplatform.campaign.domain.exception.CrawlingSourceNotFoundException;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogRepository;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlingOrchestratorSourceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CrawlingSourceRepository crawlingSourceRepository;

    @Mock
    private CrawlingLogRepository crawlingLogRepository;

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);
    private static final CrawlingSource MBLE_SOURCE =
            new CrawlingSource("MBLE", "미블", "https://www.mble.xyz", null, null, "MBLE", 2);

    @Test
    @DisplayName("executeAll - 활성 소스만 실행, 비활성 건너뜀")
    void executeAll_activeSourcesOnly() {
        CrawlingSource inactiveSource = new CrawlingSource("INACTIVE", "비활성", "https://inactive.com", null, null, "INACTIVE", 99);
        inactiveSource.deactivate();

        CampaignCrawler revuCrawler = mock(CampaignCrawler.class);
        when(revuCrawler.getCrawlerType()).thenReturn("REVU");
        when(revuCrawler.crawl(REVU_SOURCE)).thenReturn(List.of());

        // findAllActiveOrderByDisplayOrder returns only active sources
        when(crawlingSourceRepository.findAllActiveOrderByDisplayOrder()).thenReturn(List.of(REVU_SOURCE));
        when(campaignRepository.findExpiredRecruitingCampaigns(any())).thenReturn(List.of());
        when(crawlingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CrawlerRegistry registry = new CrawlerRegistry(List.of(revuCrawler));
        CrawlingOrchestrator orchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository);

        List<CrawlingResult> results = orchestrator.executeAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSourceCode()).isEqualTo("REVU");
    }

    @Test
    @DisplayName("executeAll - 매칭 크롤러 없으면 FAILED 로그 저장 후 다음 소스로 진행")
    void executeAll_noCrawlerFails() {
        // REVU source has crawlerType "REVU" but no crawler registered for it
        when(crawlingSourceRepository.findAllActiveOrderByDisplayOrder()).thenReturn(List.of(REVU_SOURCE, MBLE_SOURCE));
        when(campaignRepository.findExpiredRecruitingCampaigns(any())).thenReturn(List.of());
        when(crawlingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Only register MBLE crawler, not REVU
        CampaignCrawler mbleCrawler = mock(CampaignCrawler.class);
        when(mbleCrawler.getCrawlerType()).thenReturn("MBLE");
        when(mbleCrawler.crawl(MBLE_SOURCE)).thenReturn(List.of());

        CrawlerRegistry registry = new CrawlerRegistry(List.of(mbleCrawler));
        CrawlingOrchestrator orchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository);

        List<CrawlingResult> results = orchestrator.executeAll();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getSourceCode()).isEqualTo("REVU");
        assertThat(results.get(0).getStatus()).isEqualTo(CrawlingLogStatus.FAILED);
        assertThat(results.get(1).getSourceCode()).isEqualTo("MBLE");
        assertThat(results.get(1).getStatus()).isEqualTo(CrawlingLogStatus.SUCCESS);

        verify(crawlingLogRepository, times(2)).save(any(CrawlingLog.class));
    }

    @Test
    @DisplayName("executeBySourceCode - 성공")
    void executeBySourceCode_success() {
        CampaignCrawler revuCrawler = mock(CampaignCrawler.class);
        when(revuCrawler.getCrawlerType()).thenReturn("REVU");
        when(revuCrawler.crawl(REVU_SOURCE)).thenReturn(List.of());

        when(crawlingSourceRepository.findByCode("REVU")).thenReturn(Optional.of(REVU_SOURCE));
        when(campaignRepository.findExpiredRecruitingCampaigns(any())).thenReturn(List.of());
        when(crawlingLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CrawlerRegistry registry = new CrawlerRegistry(List.of(revuCrawler));
        CrawlingOrchestrator orchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository);

        CrawlingResult result = orchestrator.executeBySourceCode("REVU");

        assertThat(result.getSourceCode()).isEqualTo("REVU");
        assertThat(result.getStatus()).isEqualTo(CrawlingLogStatus.SUCCESS);
    }

    @Test
    @DisplayName("executeBySourceCode - 존재하지 않는 소스 → CrawlingSourceNotFoundException")
    void executeBySourceCode_notFound() {
        when(crawlingSourceRepository.findByCode("NONEXIST")).thenReturn(Optional.empty());

        CrawlerRegistry registry = new CrawlerRegistry(List.of());
        CrawlingOrchestrator orchestrator = new CrawlingOrchestrator(
                registry, campaignRepository, crawlingSourceRepository, crawlingLogRepository);

        assertThatThrownBy(() -> orchestrator.executeBySourceCode("NONEXIST"))
                .isInstanceOf(CrawlingSourceNotFoundException.class);
    }
}
