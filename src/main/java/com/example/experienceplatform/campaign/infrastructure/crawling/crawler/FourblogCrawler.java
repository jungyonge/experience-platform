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
public class FourblogCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(FourblogCrawler.class);
    private static final String BASE_URL = "https://4blog.net";
    private static final String API_URL = BASE_URL + "/loadMoreDataCategory";
    private static final String CDN_URL = "https://d3oxv6xcx9d0j1.cloudfront.net/public/pr/";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public FourblogCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "FOURBLOG";
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

        try {
            String url = API_URL + "?offset=0&limit=300&category=blog&category1=&location=&search=";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", properties.getUserAgent())
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("FOURBLOG API 응답 오류: HTTP {}", response.statusCode());
                return results;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) return results;

            for (JsonNode item : root) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("FOURBLOG 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("FOURBLOG 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("FOURBLOG 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // 4blog is a React SPA - content loads via client-side JS, not available in Jsoup HTML
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
        String cid = item.path("CID").asText("");
        String title = item.path("CAMPAIGN_NM").asText("").trim();
        if (cid.isEmpty() || title.isEmpty()) return null;

        String originalUrl = BASE_URL + "/campaign/" + cid + "/";

        String prid = item.path("PRID").asText("");
        String imgKey = item.path("IMGKEY").asText("");
        String thumbnailUrl = (prid.isEmpty() || imgKey.isEmpty()) ? null :
                CDN_URL + prid + "/thumbnail/" + imgKey;

        int reviewerCnt = item.path("REVIEWER_CNT").asInt(0);
        String benefit = item.path("REVIEWER_BENEFIT").asText(null);
        String category1 = item.path("CATEGORY1").asText("");
        String keyword = item.path("KEYWORD").asText("");

        int remainDate = item.path("REMAINDATE").asInt(0);
        LocalDate applyEndDate = remainDate > 0 ? LocalDate.now().plusDays(remainDate) : null;
        CampaignStatus status = remainDate >= 0 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;

        CampaignCategory category = CategoryMapper.map(title + " " + category1 + " " + keyword);

        return new CrawledCampaign(
                source.getCode(), cid, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                reviewerCnt > 0 ? reviewerCnt : null, null, applyEndDate, null,
                benefit, "블로그 리뷰 작성", null, "포블로그,체험단"
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
                    source.getCode(), "fourblog-" + i,
                    "[포블로그] 체험단 캠페인 #" + i,
                    "포블로그 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=FOURBLOG+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",포블로그,체험단"
            ));
        }
        return mocks;
    }
}
