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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;

@Component
public class FineadpleCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(FineadpleCrawler.class);
    private static final String BASE_URL = "https://www.fineadple.com";
    private static final String API_URL = "https://b2c-api.fineadple.com/campaign/list";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public FineadpleCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "FINEADPLE";
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

        for (int page = 0; page < properties.getMaxPagesPerSite(); page++) {
            try {
                String url = API_URL + "?page=" + page + "&size=20";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) break;

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode content = root.path("content");
                if (!content.isArray() || content.isEmpty()) break;

                for (JsonNode item : content) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("FINEADPLE 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                int totalPages = root.path("totalPages").asInt(1);
                if (page >= totalPages - 1) break;
                if (page < properties.getMaxPagesPerSite() - 1) delayHandler.delay();
            } catch (Exception e) {
                log.error("FINEADPLE 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("FINEADPLE 크롤링 완료: {}건", results.size());
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
        int campaignId = item.path("campaignId").asInt(0);
        String title = item.path("title").asText("").trim();
        if (campaignId == 0 || title.isEmpty()) return null;

        String originalId = String.valueOf(campaignId);
        String originalUrl = BASE_URL + "/campaign/detail/" + campaignId;

        String thumbnailUrl = item.path("mainImage").asText(null);
        String offerItems = item.path("offerItems").asText(null);
        int recruitNum = item.path("recruitmentPersonNumber").asInt(0);
        String channel = item.path("recruitmentChannel").asText("");

        LocalDate applyEndDate = null;
        String endDateStr = item.path("recruitmentEndDatetime").asText("");
        if (!endDateStr.isEmpty()) {
            try {
                applyEndDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
            } catch (Exception e) {
                try {
                    applyEndDate = LocalDate.parse(endDateStr.substring(0, 10));
                } catch (Exception ignored) {}
            }
        }

        CampaignCategory category = CategoryMapper.map(title);
        String mission = "INSTAGRAM".equalsIgnoreCase(channel) ? "인스타그램 리뷰 작성" : "블로그 리뷰 작성";

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitNum > 0 ? recruitNum : null, null, applyEndDate, null,
                offerItems, mission, null, "파인앳플,체험단"
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
                    source.getCode(), "fineadple-" + i,
                    "[파인앳플] 체험단 캠페인 #" + i,
                    "파인앳플 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=FINEADPLE+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",파인앳플,체험단"
            ));
        }
        return mocks;
    }
}
