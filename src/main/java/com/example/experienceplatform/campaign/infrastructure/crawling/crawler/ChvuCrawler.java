package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;

@Component
public class ChvuCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ChvuCrawler.class);
    private static final String BASE_URL = "https://chvu.co.kr";
    private static final String API_URL = "https://chvu.co.kr/v2/campaigns";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public ChvuCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                       CrawlingDelayHandler delayHandler, ObjectMapper objectMapper,
                       DetailPageEnricher enricher) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
        this.enricher = enricher;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getCrawlerType() {
        return "CHVU";
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
                String url = API_URL + "?category=newly&page=" + page + "&count=20";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("CHVU API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.path("data");

                if (!data.isArray() || data.isEmpty()) break;

                for (JsonNode item : data) {
                    try {
                        CrawledCampaign campaign = parseCampaign(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("CHVU 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                // If fewer than count items, no more pages
                if (data.size() < 20) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("CHVU 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("CHVU 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // Chvu is a Next.js SPA - content loads via client-side JS, not available in Jsoup HTML
        // Only og:description meta tag is reliably available in static HTML
        String description = null;
        Element metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                campaign.getReward(), campaign.getMission(),
                campaign.getAddress(),
                campaign.getKeywords(),
                campaign.getCurrentApplicants()
        );
    }

    private CrawledCampaign parseCampaign(JsonNode item, CrawlingSource source) {
        int campaignId = item.path("campaignId").asInt(0);
        String title = item.path("title").asText("").trim();
        if (campaignId == 0 || title.isEmpty()) return null;

        String originalId = String.valueOf(campaignId);

        String mainImg = item.path("mainImg").asText("");
        String thumbnailUrl = mainImg.isEmpty() ? null :
                (mainImg.startsWith("http") ? mainImg : BASE_URL + mainImg);

        String reward = item.path("subtitle").asText(null);
        int reviewerLimit = item.path("reviewerLimit").asInt(0);
        String channel = item.path("channel").asText("");
        String activity = item.path("activity").asText("");

        String originalUrl = BASE_URL + "/campaign/" + campaignId;

        // Parse closeAt (epoch millis)
        LocalDate applyEndDate = null;
        long closeAt = item.path("closeAt").asLong(0);
        if (closeAt > 0) {
            try {
                applyEndDate = Instant.ofEpochMilli(closeAt)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toLocalDate();
            } catch (Exception e) {
                log.debug("CHVU 날짜 변환 실패: {}", closeAt);
            }
        }

        CampaignCategory category = CategoryMapper.map(title + " " + channel + " " + activity);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                reviewerLimit > 0 ? reviewerLimit : null, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "체험뷰,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.FOOD, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "chvu-" + i,
                    "[체험뷰] 체험단 캠페인 #" + i,
                    "체험뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=CHVU+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",체험뷰,체험단"
            ));
        }
        return mocks;
    }
}
