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
public class AlljamCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(AlljamCrawler.class);
    private static final String BASE_URL = "https://www.alljam.co.kr";
    private static final String API_URL = "https://www.alljam.co.kr/experiences";

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final DetailPageEnricher enricher;
    private final HttpClient httpClient;

    public AlljamCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
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
        return "ALLJAM";
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
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("ALLJAM API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode paginator = root.path("paginator");
                JsonNode data = paginator.path("data");
                int lastPage = paginator.path("last_page").asInt(1);

                if (!data.isArray() || data.isEmpty()) break;

                for (JsonNode item : data) {
                    try {
                        CrawledCampaign campaign = parseExperience(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("ALLJAM 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                if (page >= lastPage) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("ALLJAM 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("ALLJAM 크롤링 완료: {}건", results.size());
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

    private CrawledCampaign parseExperience(JsonNode item, CrawlingSource source) {
        int id = item.path("id").asInt(0);
        String title = item.path("subject").asText("").trim();
        if (id == 0 || title.isEmpty()) return null;

        String originalId = String.valueOf(id);

        String thumbnailPath = item.path("thumbnail_url").asText("");
        String thumbnailUrl = thumbnailPath.isEmpty() ? null :
                (thumbnailPath.startsWith("http") ? thumbnailPath : BASE_URL + thumbnailPath);

        String reward = item.path("services").asText(null);
        int capacity = item.path("capacity").asInt(0);
        boolean isExpired = item.path("is_expired").asBoolean(false);

        String originalUrl = BASE_URL + "/experiences/" + id;

        // Collect review type names for category mapping
        StringBuilder reviewTypes = new StringBuilder();
        JsonNode reviewTypesNode = item.path("review_types");
        if (reviewTypesNode.isArray()) {
            for (JsonNode rt : reviewTypesNode) {
                String name = rt.path("name").asText("");
                if (!name.isEmpty()) {
                    if (reviewTypes.length() > 0) reviewTypes.append(" ");
                    reviewTypes.append(name);
                }
            }
        }

        CampaignStatus status = isExpired ? CampaignStatus.CLOSED : CampaignStatus.RECRUITING;
        CampaignCategory category = CategoryMapper.map(title + " " + reviewTypes);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                capacity > 0 ? capacity : null, null, null, null,
                reward, "블로그 리뷰 작성", null, "잠자리,체험단"
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
                    source.getCode(), "alljam-" + i,
                    "[잠자리] 체험단 캠페인 #" + i,
                    "잠자리 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=ALLJAM+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",잠자리,체험단"
            ));
        }
        return mocks;
    }
}
