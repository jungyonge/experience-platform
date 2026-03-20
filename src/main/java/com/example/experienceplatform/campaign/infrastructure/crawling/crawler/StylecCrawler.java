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
public class StylecCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(StylecCrawler.class);
    private static final String BASE_URL = "https://www.stylec.co.kr";
    private static final String API_URL = "https://api2.stylec.co.kr:6439/v1/trial";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public StylecCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                          RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler,
                          ObjectMapper objectMapper, DetailPageEnricher enricher) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
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
        return "STYLEC";
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
                // StyleC REST API: GET /v1/trial?page={page}&count=20
                String url = API_URL + "?page=" + page + "&count=20&order=latest";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", properties.getUserAgent())
                        .header("Accept", "application/json")
                        .header("Referer", BASE_URL)
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("STYLEC API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                boolean success = root.path("success").asBoolean(false);
                if (!success) {
                    log.warn("STYLEC API 응답 실패: {}", root.path("message").asText(""));
                    break;
                }

                JsonNode data = root.path("data").path("data");
                if (!data.isArray() || data.isEmpty()) break;

                for (JsonNode item : data) {
                    try {
                        CrawledCampaign campaign = parseApiItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("STYLEC 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                // 다음 페이지 존재 여부 확인
                int total = root.path("data").path("Total").asInt(
                        root.path("data").path("total").asInt(0));
                if (page * 20 >= total) break;

                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("STYLEC 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("STYLEC 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // Stylec is a Vue.js SPA - content loads via client-side JS, not available in Jsoup HTML
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

    public CrawledCampaign parseApiItem(JsonNode item, CrawlingSource source) {
        int wrId = item.path("wr_id").asInt(0);
        if (wrId == 0) return null;

        String title = item.path("wr_subject").asText("").trim();
        if (title.isEmpty()) return null;

        String originalId = String.valueOf(wrId);

        // 링크 구성
        String link = item.path("link").asText("");
        String originalUrl;
        if (link.startsWith("http")) {
            originalUrl = link;
        } else if (!link.isEmpty()) {
            originalUrl = BASE_URL + link;
        } else {
            originalUrl = BASE_URL + "/trial/" + wrId;
        }

        // 썸네일
        String thumbnailUrl = item.path("img").asText(null);
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
            thumbnailUrl = BASE_URL + thumbnailUrl;
        }

        // SNS 타입
        String snsType = item.path("sns_type").asText("");

        // 카테고리
        String caName = item.path("ca_name").asText("");
        String wrType = item.path("wr_type").asText("");
        CampaignCategory category = CategoryMapper.map(caName + " " + title);

        // 모집 인원
        int recruitMax = item.path("tr_recruit_max").asInt(0);

        // 마감일
        String finday = item.path("tr_recruit_finish").asText(null);
        LocalDate applyEndDate = parseDate(finday);

        // 모집 시작일
        String startDay = item.path("tr_recruit_start").asText(null);
        LocalDate applyStartDate = parseDate(startDay);

        // D-day로 상태 판별
        int dday = item.path("dday").asInt(0);
        CampaignStatus status = dday < 0 ? CampaignStatus.CLOSED : CampaignStatus.RECRUITING;

        // 보상
        String cashbackAmt = item.path("tr_cashback_amt").asText("0");
        String reward = null;
        try {
            long cashAmount = Long.parseLong(cashbackAmt);
            if (cashAmount > 0) {
                reward = String.format("%,d원", cashAmount);
            }
        } catch (NumberFormatException ignored) {
        }

        // 미션
        String mission = "블로그 리뷰 작성";
        if (snsType.contains("instagram")) mission = "인스타그램 리뷰 작성";
        else if (snsType.contains("tiktok")) mission = "틱톡 리뷰 작성";

        // 키워드
        List<String> keywords = new ArrayList<>();
        if (!snsType.isEmpty()) keywords.add(snsType);
        if (!wrType.isEmpty()) keywords.add(wrType);
        if (!caName.isEmpty()) keywords.add(caName);
        keywords.add("스타일씨");
        keywords.add("체험단");

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitMax > 0 ? recruitMax : null,
                applyStartDate, applyEndDate, null,
                reward, mission, null, String.join(",", keywords)
        );
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // "2024-07-15 00:00:00" 형식
            if (dateStr.contains(" ")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
            }
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
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
                    source.getCode(), "stylec-" + i,
                    "[스타일씨] 체험단 캠페인 #" + i,
                    "스타일씨 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=STYLEC+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",스타일씨,체험단"
            ));
        }
        return mocks;
    }
}
