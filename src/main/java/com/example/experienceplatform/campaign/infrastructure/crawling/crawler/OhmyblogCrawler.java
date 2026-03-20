package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class OhmyblogCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(OhmyblogCrawler.class);
    private static final String BASE_URL = "https://ohmyblog.co.kr";
    private static final String API_URL = "https://ohmyblog.co.kr/api/web/campaign/active";

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final DetailPageEnricher enricher;
    private final HttpClient httpClient;

    public OhmyblogCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
                           ObjectMapper objectMapper, DetailPageEnricher enricher) {
        this.properties = properties;
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
        return "OHMYBLOG";
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
                    log.error("OHMYBLOG API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                String result = root.path("result").asText("");
                if (!"Y".equals(result)) {
                    log.warn("OHMYBLOG API 결과 실패: {}", result);
                    break;
                }

                JsonNode data = root.path("data");
                JsonNode campaigns = data.path("campaigns");
                int totalPages = data.path("totalPages").asInt(1);

                if (!campaigns.isArray() || campaigns.isEmpty()) break;

                for (JsonNode campaign : campaigns) {
                    try {
                        CrawledCampaign parsed = parseCampaign(campaign, source);
                        if (parsed != null) results.add(parsed);
                    } catch (Exception e) {
                        log.warn("OHMYBLOG 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                if (page >= totalPages) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("OHMYBLOG 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("OHMYBLOG 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        String description = null;
        Element metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                campaign.getReward(), campaign.getMission(),
                campaign.getAddress(), campaign.getKeywords(),
                campaign.getCurrentApplicants()
        );
    }

    private CrawledCampaign parseCampaign(JsonNode campaign, CrawlingSource source) {
        String appSeq = campaign.path("app_seq").asText("").trim();
        String title = campaign.path("app_title").asText("").trim();
        if (appSeq.isEmpty() || title.isEmpty()) return null;

        String thumbnail = campaign.path("thumbnail").asText("");
        String thumbnailUrl = thumbnail.isEmpty() ? null :
                (thumbnail.startsWith("http") ? thumbnail : BASE_URL + thumbnail);

        int recruitCount = campaign.path("app_recruitCount").asInt(0);
        String reward = campaign.path("supplyItem").asText(null);
        String typeText = campaign.path("app_type_text").asText("");
        String companyName = campaign.path("app_companyName").asText("");

        String originalUrl = BASE_URL + "/user/productDetail?app_seq=" + appSeq;

        // Parse deadline (yyyy-MM-dd)
        LocalDate applyEndDate = null;
        String endDate = campaign.path("app_recruitEndDate").asText("");
        if (!endDate.isEmpty()) {
            try {
                applyEndDate = LocalDate.parse(endDate.substring(0, 10));
            } catch (Exception e) {
                log.debug("OHMYBLOG 날짜 파싱 실패: {}", endDate);
            }
        }

        CampaignCategory category = CategoryMapper.map(title + " " + typeText);

        return new CrawledCampaign(
                source.getCode(), appSeq, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount > 0 ? recruitCount : null, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "오마이블로그,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 10; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "ohmyblog-" + i,
                    "[오마이블로그] 체험단 캠페인 #" + i,
                    "오마이블로그 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=OHMYBLOG+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",오마이블로그,체험단"
            ));
        }
        return mocks;
    }
}
