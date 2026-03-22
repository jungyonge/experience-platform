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
public class BlogdexCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(BlogdexCrawler.class);
    private static final String BASE_URL = "https://blogdexreview.space";
    private static final String API_URL = "https://blogdexreview.space/api/trials";

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final DetailPageEnricher enricher;
    private final HttpClient httpClient;

    public BlogdexCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
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
        return "BLOGDEX";
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
                String url = API_URL + "?page=" + page + "&sort=latest";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("BLOGDEX API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.path("data");
                JsonNode trials = data.path("trials");

                if (!trials.isArray() || trials.isEmpty()) break;

                for (JsonNode trial : trials) {
                    try {
                        CrawledCampaign campaign = parseTrial(trial, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("BLOGDEX 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                boolean hasMore = data.path("hasMore").asBoolean(false);
                if (!hasMore) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("BLOGDEX 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("BLOGDEX 크롤링 완료: {}건", results.size());
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
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseTrial(JsonNode trial, CrawlingSource source) {
        String trialId = trial.path("trialId").asText("");
        String title = trial.path("serviceName").asText("").trim();
        if (trialId.isEmpty() || title.isEmpty()) return null;

        String thumbnailUrl = trial.path("thumbnailImageUrl").asText(null);
        String reward = trial.path("provide").asText(null);
        int recruitCount = trial.path("participantCount").asInt(0);
        String address = trial.path("serviceLocation").asText(null);

        String originalUrl = BASE_URL + "/trials/" + trialId;

        // Parse deadline (ISO8601)
        LocalDate applyEndDate = null;
        String deadline = trial.path("recruitDeadline").asText("");
        if (!deadline.isEmpty()) {
            try {
                applyEndDate = LocalDate.parse(deadline.substring(0, 10));
            } catch (Exception e) {
                log.debug("BLOGDEX 날짜 파싱 실패: {}", deadline);
            }
        }

        // Map status
        String trialStatus = trial.path("trialStatus").asText("");
        CampaignStatus status = "recruiting".equalsIgnoreCase(trialStatus)
                ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), trialId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount > 0 ? recruitCount : null, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", address, "블덱스,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "blogdex-" + i,
                    "[블덱스] 체험단 캠페인 #" + i,
                    "블덱스 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=BLOGDEX+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",블덱스,체험단"
            ));
        }
        return mocks;
    }
}
