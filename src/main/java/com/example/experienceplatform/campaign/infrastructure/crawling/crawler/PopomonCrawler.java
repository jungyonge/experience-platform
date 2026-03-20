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
public class PopomonCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(PopomonCrawler.class);
    private static final String BASE_URL = "https://popomon.com";
    private static final String API_URL = BASE_URL + "/api_p/campaign/fetch_getcampaignlist";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public PopomonCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "POPOMON";
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
                String url = API_URL + "?pageNum=" + page;
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
                if (!root.path("success").asBoolean(false)) break;

                JsonNode data = root.path("data");
                JsonNode contents = data.path("contentsData");
                if (!contents.isArray() || contents.isEmpty()) break;

                for (JsonNode item : contents) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("POPOMON 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                int totalCount = Integer.parseInt(data.path("campCount").asText("0"));
                if (page * 12 >= totalCount) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("POPOMON 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("POPOMON 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // Popomon is a Next.js SPA - content loads via client-side JS, not available in Jsoup HTML
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
        String cIdx = item.path("C_idx").asText("");
        String title = item.path("C_title").asText("").trim();
        if (cIdx.isEmpty() || title.isEmpty()) return null;

        String originalUrl = BASE_URL + "/campaign/" + cIdx;
        String thumbnailUrl = item.path("thumb_img").asText(null);
        String provision = item.path("C_provision").asText(null);
        int choiceCount = item.path("C_choice_count").asInt(0);
        String ctType = item.path("CT_type").asText("");
        String recruitType = item.path("C_recruit_type").asText("");

        LocalDate applyEndDate = null;
        String endDate = item.path("C_regi_end_date").asText("");
        if (!endDate.isEmpty()) {
            try {
                applyEndDate = LocalDate.parse(endDate);
            } catch (Exception ignored) {}
        }

        String state = item.path("C_state").asText("");
        CampaignStatus status = "ONGOING".equals(state) ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
        CampaignCategory category = CategoryMapper.map(ctType + " " + recruitType + " " + title);

        return new CrawledCampaign(
                source.getCode(), cIdx, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                choiceCount > 0 ? choiceCount : null, null, applyEndDate, null,
                provision, "블로그 리뷰 작성", null, "포포몬,체험단"
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
                    source.getCode(), "popomon-" + i,
                    "[포포몬] 체험단 캠페인 #" + i,
                    "포포몬 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=POPOMON+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",포포몬,체험단"
            ));
        }
        return mocks;
    }
}
