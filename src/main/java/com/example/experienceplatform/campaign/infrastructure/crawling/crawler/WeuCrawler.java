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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;

@Component
public class WeuCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(WeuCrawler.class);
    private static final String BASE_URL = "https://weu.kr";
    private static final String API_URL = "https://api.weu.kr/api/v1/user/slot/campaign-winner-trynow";
    private static final String CDN_URL = "https://cdn.weu.kr";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public WeuCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "WEU";
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
                String url = API_URL + "?type=all&per_page=20&offset=" + page;
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
                JsonNode data = root.path("data");
                if (!data.isArray() || data.isEmpty()) break;

                for (JsonNode item : data) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("WEU 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                int total = root.path("total").asInt(0);
                if (page * 20 >= total) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("WEU 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("WEU 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // Weu is a React SPA - content loads via client-side JS, not available in Jsoup HTML
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

    private CrawledCampaign parseItem(JsonNode item, CrawlingSource source) {
        int id = item.path("id").asInt(0);
        String title = item.path("campaign_title").asText("").trim();
        if (id == 0 || title.isEmpty()) return null;

        String originalId = String.valueOf(id);
        String type = item.path("type").asText("trynow");
        String originalUrl = BASE_URL + "/detailPage/" + id + "?type=" + type;

        String thumbnail = item.path("thumbnail").asText("");
        String thumbnailUrl = thumbnail.isEmpty() ? null :
                (thumbnail.startsWith("http") ? thumbnail : CDN_URL + thumbnail);

        int maxSelected = item.path("max_selected").asInt(0);
        String campaignType = item.path("campaign_type").asText("");

        LocalDate applyEndDate = null;
        String endDateStr = item.path("registration_end_date").asText("");
        if (!endDateStr.isEmpty()) {
            try {
                String datePart = endDateStr.split("-")[0];
                applyEndDate = LocalDate.parse(datePart, DATE_FMT);
            } catch (Exception e) {
                log.debug("WEU 날짜 파싱 실패: {}", endDateStr);
            }
        }

        CampaignCategory category = CategoryMapper.map(title + " " + campaignType);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                maxSelected > 0 ? maxSelected : null, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, "위유,체험단"
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
                    source.getCode(), "weu-" + i,
                    "[위유] 체험단 캠페인 #" + i,
                    "위유 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=WEU+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",위유,체험단"
            ));
        }
        return mocks;
    }
}
