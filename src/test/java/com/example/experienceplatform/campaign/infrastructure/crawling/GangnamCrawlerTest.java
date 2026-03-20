package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.crawler.GangnamCrawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GangnamCrawlerTest {

    private static final CrawlingSource GANGNAM_SOURCE =
            new CrawlingSource("GANGNAM", "강남맛집", "https://xn--939au0g4vj8sq.net", null, null, "GANGNAM", 3);

    @Test
    @DisplayName("HTML fixture 파싱 - 유효 캠페인 아이템만 반환")
    void parseHtml() throws IOException {
        Document doc = loadFixture("crawling/gangnam_sample.html");
        CrawlingProperties props = mock(CrawlingProperties.class);
        when(props.isMockEnabled()).thenReturn(true);
        GangnamCrawler crawler = new GangnamCrawler(props, null, null, null, null);

        Elements items = doc.select("li:has(a[href*=/cp/?id=])");
        List<CrawledCampaign> results = new ArrayList<>();
        for (Element item : items) {
            CrawledCampaign c = crawler.parseItem(item, GANGNAM_SOURCE);
            if (c != null) results.add(c);
        }

        assertThat(results).hasSize(2);

        CrawledCampaign first = results.get(0);
        assertThat(first.getOriginalId()).isEqualTo("2102500");
        assertThat(first.getTitle()).isEqualTo("[서울 강서] 도담");
        assertThat(first.getRecruitCount()).isEqualTo(10);
        assertThat(first.getStatus()).isEqualTo(CampaignStatus.RECRUITING);
        assertThat(first.getCategory()).isEqualTo(CampaignCategory.FOOD);
        assertThat(first.getThumbnailUrl()).startsWith("https://");
        assertThat(first.getReward()).isEqualTo("4만원 (2인기준) 체험권");
        assertThat(first.getMission()).isEqualTo("방문 체험 후 블로그 리뷰");
        assertThat(first.getApplyEndDate()).isNotNull();

        CrawledCampaign second = results.get(1);
        assertThat(second.getOriginalId()).isEqualTo("2102501");
        assertThat(second.getTitle()).isEqualTo("[서울 마포] 맛있는 초밥");
        assertThat(second.getRecruitCount()).isEqualTo(5);
        assertThat(second.getMission()).isEqualTo("블로그 리뷰");
    }

    @Test
    @DisplayName("mock 모드 데이터 생성 - 10건, sourceCode = GANGNAM")
    void mockData() {
        CrawlingProperties props = mock(CrawlingProperties.class);
        when(props.isMockEnabled()).thenReturn(true);
        GangnamCrawler crawler = new GangnamCrawler(props, null, null, null, null);

        List<CrawledCampaign> results = crawler.crawl(GANGNAM_SOURCE);

        assertThat(results).hasSize(10);
        assertThat(results.get(0).getSourceCode()).isEqualTo("GANGNAM");
    }

    @Test
    @DisplayName("빈 HTML → 빈 리스트")
    void emptyHtml() {
        Document doc = Jsoup.parse("<html><body></body></html>");
        Elements items = doc.select("li:has(a[href*=/cp/?id=])");
        assertThat(items).isEmpty();
    }

    private Document loadFixture(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return Jsoup.parse(html);
    }
}
