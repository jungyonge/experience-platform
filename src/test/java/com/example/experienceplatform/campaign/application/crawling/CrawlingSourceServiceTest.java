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
import org.junit.jupiter.api.BeforeEach;
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
class CrawlingSourceServiceTest {

    @Mock
    private CrawlingSourceRepository crawlingSourceRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CrawlingLogRepository crawlingLogRepository;

    @Mock
    private CrawlerRegistry crawlerRegistry;

    private CrawlingSourceService service;

    @BeforeEach
    void setUp() {
        service = new CrawlingSourceService(
                crawlingSourceRepository, campaignRepository, crawlingLogRepository, crawlerRegistry);
    }

    @Test
    @DisplayName("create - 성공")
    void create_success() {
        CrawlingSourceCreateCommand command = new CrawlingSourceCreateCommand(
                "NEW_SRC", "새소스", "https://new.com", null, "설명", "NEW", 10);

        when(crawlingSourceRepository.existsByCode("NEW_SRC")).thenReturn(false);
        when(crawlingSourceRepository.save(any(CrawlingSource.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.countByCrawlingSource(any())).thenReturn(0L);
        when(crawlingLogRepository.findLatestBySourceCode(any())).thenReturn(Optional.empty());

        CrawlingSourceInfo info = service.create(command);

        assertThat(info.getCode()).isEqualTo("NEW_SRC");
        assertThat(info.getName()).isEqualTo("새소스");
        assertThat(info.isActive()).isTrue();
        verify(crawlingSourceRepository).save(any(CrawlingSource.class));
    }

    @Test
    @DisplayName("create - 중복 코드 → DuplicateSourceCodeException")
    void create_duplicateCode() {
        CrawlingSourceCreateCommand command = new CrawlingSourceCreateCommand(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);

        when(crawlingSourceRepository.existsByCode("REVU")).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(DuplicateSourceCodeException.class);

        verify(crawlingSourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - 성공")
    void update_success() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);
        CrawlingSourceUpdateCommand command = new CrawlingSourceUpdateCommand(
                "레뷰v2", "https://revu-v2.net", "/list", "업데이트", "REVU_V2", 5);

        when(crawlingSourceRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlingSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.countByCrawlingSource(any())).thenReturn(0L);
        when(crawlingLogRepository.findLatestBySourceCode(any())).thenReturn(Optional.empty());

        CrawlingSourceInfo info = service.update(1L, command);

        assertThat(info.getName()).isEqualTo("레뷰v2");
        assertThat(info.getBaseUrl()).isEqualTo("https://revu-v2.net");
        assertThat(info.getCrawlerType()).isEqualTo("REVU_V2");
    }

    @Test
    @DisplayName("update - 존재하지 않는 소스 → CrawlingSourceNotFoundException")
    void update_notFound() {
        CrawlingSourceUpdateCommand command = new CrawlingSourceUpdateCommand(
                "이름", "https://url.com", null, null, "TYPE", 1);

        when(crawlingSourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, command))
                .isInstanceOf(CrawlingSourceNotFoundException.class);
    }

    @Test
    @DisplayName("toggleActive - 활성 → 비활성")
    void toggleActive_activeToInactive() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);
        assertThat(source.isActive()).isTrue();

        when(crawlingSourceRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlingSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.countByCrawlingSource(any())).thenReturn(0L);
        when(crawlingLogRepository.findLatestBySourceCode(any())).thenReturn(Optional.empty());

        CrawlingSourceInfo info = service.toggleActive(1L);

        assertThat(info.isActive()).isFalse();
    }

    @Test
    @DisplayName("toggleActive - 비활성 → 활성")
    void toggleActive_inactiveToActive() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);
        source.deactivate();
        assertThat(source.isActive()).isFalse();

        when(crawlingSourceRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlingSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.countByCrawlingSource(any())).thenReturn(0L);
        when(crawlingLogRepository.findLatestBySourceCode(any())).thenReturn(Optional.empty());

        CrawlingSourceInfo info = service.toggleActive(1L);

        assertThat(info.isActive()).isTrue();
    }

    @Test
    @DisplayName("testCrawl - 성공")
    void testCrawl_success() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "REVU", 1);
        CampaignCrawler crawler = mock(CampaignCrawler.class);

        when(crawlingSourceRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlerRegistry.findByCrawlerType("REVU")).thenReturn(Optional.of(crawler));
        when(crawler.crawl(source)).thenReturn(List.of());

        CrawlingTestResult result = service.testCrawl(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSourceCode()).isEqualTo("REVU");
    }

    @Test
    @DisplayName("testCrawl - 크롤러 없음 → CrawlerNotFoundException")
    void testCrawl_crawlerNotFound() {
        CrawlingSource source = new CrawlingSource(
                "REVU", "레뷰", "https://revu.net", null, null, "UNKNOWN_TYPE", 1);

        when(crawlingSourceRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlerRegistry.findByCrawlerType("UNKNOWN_TYPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.testCrawl(1L))
                .isInstanceOf(CrawlerNotFoundException.class);
    }
}
