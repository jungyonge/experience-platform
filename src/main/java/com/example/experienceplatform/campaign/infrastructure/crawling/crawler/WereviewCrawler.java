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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WereviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(WereviewCrawler.class);
    private static final String BASE_URL = "https://www.wereview.fun";
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final DetailPageEnricher enricher;

    public WereviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                            RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler,
                            ObjectMapper objectMapper, DetailPageEnricher enricher) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
        this.enricher = enricher;
    }

    @Override
    public String getCrawlerType() {
        return "WEREVIEW";
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
            log.warn("WEREVIEW robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            // Next.js SSR 페이지에서 __NEXT_DATA__ JSON 추출하여 캠페인 데이터 파싱
            // campaign-list 페이지에 SSR 데이터가 포함될 수 있음
            Document doc = jsoupClient.fetch(BASE_URL + "/campaign-list");
            String html = doc.html();

            Matcher matcher = NEXT_DATA_PATTERN.matcher(html);
            if (!matcher.find()) {
                log.info("WEREVIEW __NEXT_DATA__를 찾을 수 없습니다. 숏폼 SPA로 클라이언트 사이드 렌더링 방식입니다.");
                return results;
            }

            String jsonStr = matcher.group(1);
            JsonNode root = objectMapper.readTree(jsonStr);

            // dehydrated state에서 캠페인 데이터 추출
            JsonNode dehydratedState = root.path("props").path("pageProps").path("dehydratedState");
            JsonNode queries = dehydratedState.path("queries");

            if (queries.isArray()) {
                for (JsonNode query : queries) {
                    JsonNode data = query.path("state").path("data");
                    parseCampaignData(data, source, results);
                }
            }

            // pageProps에서 직접 캠페인 데이터 확인
            if (results.isEmpty()) {
                JsonNode pageProps = root.path("props").path("pageProps");
                parseCampaignData(pageProps, source, results);
            }

        } catch (Exception e) {
            log.error("WEREVIEW 크롤링 실패: {}", e.getMessage());
        }

        if (results.isEmpty()) {
            log.info("WEREVIEW 숏폼 전문 체험단 사이트로 API 기반 클라이언트 렌더링 방식입니다. SSR 데이터가 없습니다.");
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("WEREVIEW 크롤링 완료: {}건", results.size());
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

    private void parseCampaignData(JsonNode data, CrawlingSource source, List<CrawledCampaign> results) {
        if (data == null || data.isMissingNode()) return;

        // 배열 직접 확인
        JsonNode content = data.path("content");
        if (!content.isArray()) {
            content = data.path("campaigns");
        }
        if (!content.isArray()) {
            content = data.path("data");
        }
        if (!content.isArray()) {
            // data 자체가 배열인 경우
            if (data.isArray()) content = data;
            else return;
        }

        for (JsonNode item : content) {
            try {
                CrawledCampaign campaign = parseCampaignItem(item, source);
                if (campaign != null) results.add(campaign);
            } catch (Exception e) {
                log.warn("WEREVIEW 캠페인 아이템 파싱 실패: {}", e.getMessage());
            }
        }
    }

    private CrawledCampaign parseCampaignItem(JsonNode item, CrawlingSource source) {
        // 다양한 ID 필드명 시도
        String originalId = "";
        for (String field : new String[]{"campaignId", "id", "campaignNo"}) {
            String val = item.path(field).asText("");
            if (!val.isEmpty() && !val.equals("0")) {
                originalId = val;
                break;
            }
        }
        if (originalId.isEmpty()) return null;

        // 제목
        String title = "";
        for (String field : new String[]{"title", "campaignTitle", "name"}) {
            title = item.path(field).asText("");
            if (!title.isEmpty()) break;
        }
        if (title.isEmpty()) return null;

        String originalUrl = BASE_URL + "/campaign/" + originalId;

        // 이미지
        String thumbnailUrl = item.path("mainImage").asText(null);
        if (thumbnailUrl == null) thumbnailUrl = item.path("thumbnailUrl").asText(null);
        if (thumbnailUrl == null) thumbnailUrl = item.path("imageUrl").asText(null);

        // 날짜
        String endDateStr = item.path("campaignApplyEndDate").asText(null);
        if (endDateStr == null) endDateStr = item.path("applyEndDate").asText(null);
        LocalDate applyEndDate = parseDate(endDateStr);

        String startDateStr = item.path("campaignApplyStartDate").asText(null);
        if (startDateStr == null) startDateStr = item.path("applyStartDate").asText(null);
        LocalDate applyStartDate = parseDate(startDateStr);

        CampaignStatus status = CampaignStatus.RECRUITING;
        if (applyEndDate != null && applyEndDate.isBefore(LocalDate.now())) {
            status = CampaignStatus.CLOSED;
        }

        // 모집 인원
        int recruitCount = item.path("recruitmentPersonNumber").asInt(0);
        if (recruitCount == 0) recruitCount = item.path("recruitCount").asInt(0);

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount > 0 ? recruitCount : null,
                applyStartDate, applyEndDate, null,
                null, "숏폼 영상 리뷰 작성", null, "위리뷰,체험단"
        );
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
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

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "wereview-" + i,
                    "[위리뷰] 체험단 캠페인 #" + i,
                    "위리뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=WEREVIEW+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "숏폼 영상 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",위리뷰,체험단"
            ));
        }
        return mocks;
    }
}
