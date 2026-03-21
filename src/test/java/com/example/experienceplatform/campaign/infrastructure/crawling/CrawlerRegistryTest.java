package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CrawlingSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrawlerRegistryTest {

    @Test
    @DisplayName("findByCrawlerType - 매칭되는 크롤러 반환")
    void findByCrawlerType_found() {
        CampaignCrawler revuCrawler = mock(CampaignCrawler.class);
        CampaignCrawler gangnamCrawler = mock(CampaignCrawler.class);
        when(revuCrawler.getCrawlerType()).thenReturn("REVU");
        when(gangnamCrawler.getCrawlerType()).thenReturn("GANGNAM");

        CrawlerRegistry registry = new CrawlerRegistry(List.of(revuCrawler, gangnamCrawler));

        Optional<CampaignCrawler> result = registry.findByCrawlerType("REVU");

        assertThat(result).isPresent();
        assertThat(result.get().getCrawlerType()).isEqualTo("REVU");
    }

    @Test
    @DisplayName("findByCrawlerType - 매칭 없으면 empty")
    void findByCrawlerType_notFound() {
        CampaignCrawler revuCrawler = mock(CampaignCrawler.class);
        when(revuCrawler.getCrawlerType()).thenReturn("REVU");

        CrawlerRegistry registry = new CrawlerRegistry(List.of(revuCrawler));

        Optional<CampaignCrawler> result = registry.findByCrawlerType("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAvailableCrawlerTypes - 모든 타입 반환 (정렬됨)")
    void getAvailableCrawlerTypes() {
        CampaignCrawler revuCrawler = mock(CampaignCrawler.class);
        CampaignCrawler gangnamCrawler = mock(CampaignCrawler.class);
        when(revuCrawler.getCrawlerType()).thenReturn("REVU");
        when(gangnamCrawler.getCrawlerType()).thenReturn("GANGNAM");

        CrawlerRegistry registry = new CrawlerRegistry(List.of(revuCrawler, gangnamCrawler));

        List<String> types = registry.getAvailableCrawlerTypes();

        assertThat(types).hasSize(2);
        assertThat(types).containsExactly("GANGNAM", "REVU");
    }
}
