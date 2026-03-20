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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class GugudasCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(GugudasCrawler.class);
    private static final String BASE_URL = "https://99das.com";
    private static final String API_URL = "https://99das.com/amz/list/cmpnList.do";
    private static final String CDN_PREFIX = "https://d26jvdwwu11rjl.cloudfront.net/";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String[] CATEGORIES = {"AMZ027.001", "AMZ027.002"};

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final DetailPageEnricher enricher;
    private final HttpClient httpClient;

    public GugudasCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
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
        return "GUGUDAS";
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

        for (String categoryCode : CATEGORIES) {
            for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
                try {
                    String body = "cmpnDcd=" + categoryCode + "&pageNum=" + page
                            + "&pageSize=20&type=cmpnList&tagId=cmpnList";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Accept", "application/json")
                            .header("User-Agent", properties.getUserAgent())
                            .header("Referer", BASE_URL)
                            .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        log.error("GUGUDAS API 응답 오류: HTTP {} (category={})", response.statusCode(), categoryCode);
                        break;
                    }

                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode list = root.path("list");

                    if (!list.isArray() || list.isEmpty()) break;

                    for (JsonNode item : list) {
                        try {
                            CrawledCampaign campaign = parseItem(item, source);
                            if (campaign != null) results.add(campaign);
                        } catch (Exception e) {
                            log.warn("GUGUDAS 아이템 파싱 실패: {}", e.getMessage());
                        }
                    }

                    // If fewer than pageSize items returned, no more pages
                    if (list.size() < 20) break;
                    if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
                } catch (Exception e) {
                    log.error("GUGUDAS 페이지 {} (category={}) 크롤링 실패: {}", page, categoryCode, e.getMessage());
                    break;
                }
            }
            delayHandler.delay();
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("GUGUDAS 크롤링 완료: {}건", results.size());
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

    private CrawledCampaign parseItem(JsonNode item, CrawlingSource source) {
        String cmpnId = item.path("cmpnId").asText("").trim();
        String title = item.path("cmpnNm").asText("").trim();
        if (cmpnId.isEmpty() || title.isEmpty()) return null;

        String reward = item.path("oferBrekdn").asText(null);
        String mainImgPath = item.path("mainImgPath").asText("");
        String thumbnailUrl = mainImgPath.isEmpty() ? null :
                (mainImgPath.startsWith("http") ? mainImgPath : CDN_PREFIX + mainImgPath);

        int recruitCount = item.path("recrtCnt").asInt(0);
        String areaName = item.path("cmpnAreaDcdNm").asText(null);
        String kindName = item.path("cmpnKindDcdNm").asText("");

        String originalUrl = BASE_URL + "/amz/cmpn/amzCmpnDtl.do?cmpnId=" + cmpnId;

        // Parse deadline (yyyyMMdd format)
        LocalDate applyEndDate = null;
        String endDate = item.path("recrtEnDy").asText("");
        if (!endDate.isEmpty()) {
            try {
                applyEndDate = LocalDate.parse(endDate, DATE_FORMAT);
            } catch (Exception e) {
                log.debug("GUGUDAS 날짜 파싱 실패: {}", endDate);
            }
        }

        CampaignCategory category = CategoryMapper.map(title + " " + kindName);

        return new CrawledCampaign(
                source.getCode(), cmpnId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount > 0 ? recruitCount : null, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", areaName, "구구다스,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "gugudas-" + i,
                    "[구구다스] 체험단 캠페인 #" + i,
                    "구구다스 체험단 설명 " + i,
                    "구구다스 상세 내용 " + i,
                    "https://placehold.co/300x200?text=GUGUDAS+" + i,
                    BASE_URL + "/campaign/view.do?id=gugudas-" + i,
                    cat, status,
                    5 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",구구다스,체험단"
            ));
        }
        return mocks;
    }
}
