package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FineadpleCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(FineadpleCrawler.class);
    private static final String BASE_URL = "https://www.fineadple.com";
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;

    public FineadpleCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                            RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler,
                            ObjectMapper objectMapper) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
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
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/")) {
            log.warn("FINEADPLE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            // Next.js SSR 페이지에서 __NEXT_DATA__ JSON을 추출하여 캠페인 데이터 파싱
            Document doc = jsoupClient.fetch(BASE_URL);
            String html = doc.html();

            Matcher matcher = NEXT_DATA_PATTERN.matcher(html);
            if (!matcher.find()) {
                log.warn("FINEADPLE __NEXT_DATA__를 찾을 수 없습니다.");
                return results;
            }

            String jsonStr = matcher.group(1);
            JsonNode root = objectMapper.readTree(jsonStr);

            // React Query dehydrated state에서 캠페인 데이터 추출
            JsonNode dehydratedState = root.path("props").path("pageProps").path("dehydratedState");
            if (dehydratedState.isMissingNode()) {
                dehydratedState = root.path("props").path("pageProps");
            }

            JsonNode queries = dehydratedState.path("queries");
            if (queries.isArray()) {
                for (JsonNode query : queries) {
                    String queryKey = query.path("queryKey").toString();
                    if (queryKey.contains("CAMPAIGN") || queryKey.contains("campaign")) {
                        JsonNode data = query.path("state").path("data");
                        parseCampaignData(data, source, results);
                    }
                }
            }

        } catch (Exception e) {
            log.error("FINEADPLE 크롤링 실패: {}", e.getMessage());
        }

        log.info("FINEADPLE 크롤링 완료: {}건", results.size());
        return results;
    }

    private void parseCampaignData(JsonNode data, CrawlingSource source, List<CrawledCampaign> results) {
        // 페이지네이션 응답 구조: { content: [...], pageable: {...} }
        JsonNode content = data.path("content");
        if (!content.isArray()) {
            content = data;  // 단일 배열일 수 있음
        }
        if (!content.isArray()) return;

        for (JsonNode item : content) {
            try {
                CrawledCampaign campaign = parseCampaignItem(item, source);
                if (campaign != null) results.add(campaign);
            } catch (Exception e) {
                log.warn("FINEADPLE 캠페인 아이템 파싱 실패: {}", e.getMessage());
            }
        }
    }

    private CrawledCampaign parseCampaignItem(JsonNode item, CrawlingSource source) {
        long campaignId = item.path("campaignId").asLong(0);
        if (campaignId == 0) return null;

        String title = item.path("title").asText("");
        if (title.isEmpty()) return null;

        String originalId = String.valueOf(campaignId);
        String mainImage = item.path("mainImage").asText(null);
        String thumbnailUrl = mainImage;

        // CloudFront CDN 이미지 URL
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
            thumbnailUrl = "https://d1xn9i6ytqs5tr.cloudfront.net" + thumbnailUrl;
        }

        // 캠페인 타입에 따른 URL 구성
        String recruitmentType = item.path("recruitmentType").asText("VISIT").toLowerCase();
        String recruitmentChannel = item.path("recruitmentChannel").asText("NAVER").toLowerCase();
        String originalUrl = BASE_URL + "/campaign/" + recruitmentType + "/" + originalId;

        // 모집 인원
        int recruitCount = item.path("recruitmentPersonNumber").asInt(0);

        // 날짜 파싱
        LocalDate applyStartDate = parseDateTime(item.path("recruitmentStartDatetime").asText(null));
        LocalDate applyEndDate = parseDateTime(item.path("recruitmentEndDatetime").asText(null));

        // 상태 판별
        CampaignStatus status = CampaignStatus.RECRUITING;
        if (applyEndDate != null && applyEndDate.isBefore(LocalDate.now())) {
            status = CampaignStatus.CLOSED;
        }

        // 제공 내역
        String offerItems = item.path("offerItems").asText(null);

        // 카테고리
        CampaignCategory category = CategoryMapper.map(title + " " + (offerItems != null ? offerItems : ""));

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount > 0 ? recruitCount : null,
                applyStartDate, applyEndDate, null,
                offerItems, "블로그 리뷰 작성", null, "파인앳플,체험단"
        );
    }

    private LocalDate parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) return null;
        try {
            // ISO 형식 "2024-01-15T00:00:00" 또는 유사 패턴
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
            }
            return LocalDate.parse(dateTimeStr.substring(0, 10));
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
                    source.getCode(), "fineadple-" + i,
                    "[파인앳플] 체험단 캠페인 #" + i,
                    "파인앳플 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=FINEADPLE+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",파인앳플,체험단"
            ));
        }
        return mocks;
    }
}
