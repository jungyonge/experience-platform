package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CherivuCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(CherivuCrawler.class);
    private static final String BASE_URL = "https://cherivu.co.kr";
    private static final String API_URL = "https://api.cherivu.co.kr/api/campaigns";

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CherivuCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getCrawlerType() {
        return "CHERIVU";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();

        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = API_URL + "?page=" + page;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("CHERIVU API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.path("data");
                int lastPage = root.path("meta").path("last_page").asInt(1);

                if (!data.isArray() || data.isEmpty()) break;

                for (JsonNode campaign : data) {
                    try {
                        CrawledCampaign parsed = parseCampaign(campaign, source);
                        if (parsed != null) results.add(parsed);
                    } catch (Exception e) {
                        log.warn("CHERIVU 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                if (page >= lastPage) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("CHERIVU 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        log.info("CHERIVU 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseCampaign(JsonNode campaign, CrawlingSource source) {
        int id = campaign.path("id").asInt(0);
        String title = campaign.path("title").asText("").trim();
        if (id == 0 || title.isEmpty()) return null;

        String originalId = String.valueOf(id);

        // Image URL from nested img object
        String imageUrl = campaign.path("img").path("url").asText(null);

        String reward = campaign.path("provide").asText(null);
        int recruitCount = campaign.path("count_application").asInt(0);
        String type = campaign.path("type").asText("");

        String originalUrl = BASE_URL + "/campaigns/" + id;

        // Parse deadline (yyyy-MM-dd)
        LocalDate applyEndDate = null;
        String finishedAt = campaign.path("application_finished_at").asText("");
        if (!finishedAt.isEmpty()) {
            try {
                applyEndDate = LocalDate.parse(finishedAt.substring(0, 10));
            } catch (Exception e) {
                log.debug("CHERIVU 날짜 파싱 실패: {}", finishedAt);
            }
        }

        // Map status
        String stateOngoing = campaign.path("state_ongoing").asText("");
        CampaignStatus status = "ONGOING_APPLICATION".equalsIgnoreCase(stateOngoing)
                ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;

        CampaignCategory category = CategoryMapper.map(title + " " + type);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                imageUrl, originalUrl, category, status,
                recruitCount > 0 ? recruitCount : null, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "체리뷰,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.FOOD, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 10; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "cherivu-" + i,
                    "[체리뷰] 체험단 캠페인 #" + i,
                    "체리뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=CHERIVU+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",체리뷰,체험단"
            ));
        }
        return mocks;
    }
}
