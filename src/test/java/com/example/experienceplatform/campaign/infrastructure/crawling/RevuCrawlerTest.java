package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.crawler.RevuCrawler;
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

class RevuCrawlerTest {

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);

    @Test
    @DisplayName("HTML fixture 파싱 - 유효 아이템만 반환")
    void parseHtml() throws IOException {
        Document doc = loadFixture("crawling/revu_sample.html");
        CrawlingProperties props = mock(CrawlingProperties.class);
        when(props.isMockEnabled()).thenReturn(true);
        RevuCrawler crawler = new RevuCrawler(props, null, null, null, null);

        Elements items = doc.select(".campaign-item, .card-item, article");
        List<CrawledCampaign> results = new ArrayList<>();
        for (Element item : items) {
            CrawledCampaign c = crawler.parseItem(item, REVU_SOURCE);
            if (c != null) results.add(c);
        }

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).contains("이탈리안");
        assertThat(results.get(0).getOriginalId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("mock 모드 데이터 생성")
    void mockData() {
        CrawlingProperties props = mock(CrawlingProperties.class);
        when(props.isMockEnabled()).thenReturn(true);
        RevuCrawler crawler = new RevuCrawler(props, null, null, null, null);

        List<CrawledCampaign> results = crawler.crawl(REVU_SOURCE);

        assertThat(results).hasSizeGreaterThanOrEqualTo(10);
        assertThat(results.get(0).getSourceCode()).isEqualTo("REVU");
    }

    @Test
    @DisplayName("빈 HTML → 빈 리스트")
    void emptyHtml() {
        Document doc = Jsoup.parse("<html><body></body></html>");
        Elements items = doc.select(".campaign-item, .card-item, article");
        assertThat(items).isEmpty();
    }

    private Document loadFixture(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return Jsoup.parse(html);
    }
}
