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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;

@Component
public class WhogiupCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(WhogiupCrawler.class);
    private static final String BASE_URL = "https://www.whogiup.com";
    private static final String API_URL = BASE_URL + "/api/list";
    private static final String[] TYPES = {"region", "product"};

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public WhogiupCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "WHOGIUP";
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

        for (String type : TYPES) {
            try {
                String body = objectMapper.writeValueAsString(
                        java.util.Map.of("type", type, "subcategory", "전체", "region", "전체", "place", 0, "sort", 0));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) continue;

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root;
                if (!items.isArray()) items = root.path("data");
                if (!items.isArray() || items.isMissingNode()) {
                    log.warn("WHOGIUP {} 예상하지 못한 응답 구조: {}", type,
                            response.body().substring(0, Math.min(200, response.body().length())));
                    continue;
                }

                for (JsonNode item : items) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("WHOGIUP 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
                delayHandler.delay();
            } catch (Exception e) {
                log.error("WHOGIUP {} 크롤링 실패: {}", type, e.getMessage());
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("WHOGIUP 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        String description = null;
        Element metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        String detailContent = DetailPageEnricher.extractDetailContent(doc);
        Integer currentApplicants = DetailPageEnricher.extractCurrentApplicants(doc);
        LocalDate announcementDate = DetailPageEnricher.extractAnnouncementDate(doc);
        LocalDate applyStartDate = DetailPageEnricher.extractApplyStartDate(doc);
        String address = DetailPageEnricher.extractAddress(doc);

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(),
                coalesce(campaign.getApplyStartDate(), applyStartDate),
                campaign.getApplyEndDate(),
                coalesce(campaign.getAnnouncementDate(), announcementDate),
                campaign.getReward(), campaign.getMission(),
                coalesce(campaign.getAddress(), address),
                campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(JsonNode item, CrawlingSource source) {
        String campaignId = item.path("campaign_id").asText("");
        String title = item.path("title").asText("").trim();
        if (campaignId.isEmpty() || title.isEmpty()) return null;

        String originalUrl = BASE_URL + "/details?idx=" + campaignId;
        String thumbnailUrl = item.path("thumbnail_path").asText(null);

        int requiredNum = item.path("required_num").asInt(0);
        String product = item.path("product").asText(null);
        String subcategory = item.path("subcategory").asText("");
        boolean ended = item.path("ended").asBoolean(false);

        LocalDate applyEndDate = null;
        String regEnd = item.path("re").asText("");
        if (!regEnd.isEmpty()) {
            try {
                applyEndDate = LocalDate.parse(regEnd);
            } catch (Exception e) {
                log.debug("WHOGIUP 날짜 파싱 실패: {}", regEnd);
            }
        }

        CampaignStatus status = ended ? CampaignStatus.CLOSED : CampaignStatus.RECRUITING;
        CampaignCategory category = CategoryMapper.map(subcategory + " " + title);

        return new CrawledCampaign(
                source.getCode(), campaignId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                requiredNum > 0 ? requiredNum : null, null, applyEndDate, null,
                product, "블로그 리뷰 작성", null, "후기업,체험단"
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
                    source.getCode(), "whogiup-" + i,
                    "[후기업] 체험단 캠페인 #" + i,
                    "후기업 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=WHOGIUP+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",후기업,체험단"
            ));
        }
        return mocks;
    }
}
