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
public class DailyviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(DailyviewCrawler.class);
    private static final String BASE_URL = "https://dailyview.kr";
    private static final Pattern DAYS_LEFT_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final String KEYWORDS_TAG = "데일리뷰,체험단";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public DailyviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "DAILYVIEW";
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
                String url = BASE_URL + "/review_campaign_list.php?json=list&page=" + page + "&orderby=cp_id+desc";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", properties.getUserAgent())
                        .header("Accept", "application/json")
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) break;

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode list = root.path("list");
                if (!list.isArray() || list.isEmpty()) break;

                for (JsonNode item : list) {
                    CrawledCampaign c = parseJsonItem(item, source);
                    if (c != null) results.add(c);
                }

                int totalPage = root.path("total_page").asInt(1);
                if (page >= totalPage) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("{} 페이지 {} 크롤링 실패: {}", getCrawlerType(), page, e.getMessage());
                break;
            }
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("{} 크롤링 완료: {}건", getCrawlerType(), results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from og:description meta
        String description = null;
        Element metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        // detailContent from tab content area
        String detailContent = null;
        Element detailEl = doc.selectFirst("#tab1");
        if (detailEl != null) detailContent = detailEl.html();

        // reward from .it_cp_reward_cut .con
        String reward = null;
        Element rewardEl = doc.selectFirst(".it_cp_reward_cut .con");
        if (rewardEl != null) reward = rewardEl.text().trim();

        // address from .basic_form li containing "주소" or "업체주소"
        String address = null;
        for (Element li : doc.select("ul.basic_form li")) {
            Element titEl = li.selectFirst(".tit_basic");
            if (titEl != null && titEl.text().contains("주소")) {
                Element contentEl = li.selectFirst(".content");
                if (contentEl != null) {
                    address = contentEl.text().trim();
                    break;
                }
            }
        }

        // currentApplicants from "신청 <span>X</span> / 모집 <span>Y</span>" in sidebar
        Integer currentApplicants = null;
        Element sidebar = doc.selectFirst(".iteminfo_right_fix");
        if (sidebar != null) {
            Matcher m = Pattern.compile("신청\\s*(\\d+)").matcher(sidebar.text());
            if (m.find()) currentApplicants = Integer.parseInt(m.group(1));
        }

        // announcementDate from "선정자 발표" strong text
        LocalDate announcementDate = null;
        for (Element strong : doc.select("strong")) {
            Element prev = strong.previousElementSibling();
            String parentText = strong.parent() != null ? strong.parent().text() : "";
            if (parentText.contains("선정자 발표") || parentText.contains("발표")) {
                String dateText = strong.text().trim();
                announcementDate = parseDateFromText(dateText);
                if (announcementDate != null) break;
            }
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(),
                coalesce(campaign.getAnnouncementDate(), announcementDate),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                coalesce(campaign.getAddress(), address),
                campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private LocalDate parseDateFromText(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher m = Pattern.compile("(\\d{2})\\.(\\d{2})").matcher(text);
        if (m.find()) {
            try {
                int month = Integer.parseInt(m.group(1));
                int day = Integer.parseInt(m.group(2));
                return LocalDate.now().withMonth(month).withDayOfMonth(day);
            } catch (Exception e) { return null; }
        }
        return null;
    }

    private CrawledCampaign parseJsonItem(JsonNode item, CrawlingSource source) {
        String cpId = item.path("cp_id").asText("");
        String title = item.path("cp_subject").asText("").trim();
        if (cpId.isEmpty() || title.isEmpty()) return null;

        String description = item.path("cp_description").asText(null);
        String thumbPath = item.path("cp_img_cut").asText("");
        String thumbnailUrl = thumbPath.isEmpty() ? null :
                (thumbPath.startsWith("http") ? thumbPath : BASE_URL + "/" + thumbPath.replaceFirst("^\\./", ""));
        String originalUrl = BASE_URL + "/review_campaign.php?cp_id=" + cpId;

        String type = item.path("cp_type_cut").asText("");
        String dayText = item.path("cp_day").asText("");

        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        if (dayText.contains("모집마감")) {
            status = CampaignStatus.CLOSED;
        } else {
            Matcher m = DAYS_LEFT_PATTERN.matcher(dayText);
            if (m.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(m.group(1)));
            }
        }

        Integer recruitCount = parseIntSafe(item.path("cp_recruit_cut").asText(null));
        String reward = item.path("cp_reward_cut").asText(null);

        CampaignCategory category = CategoryMapper.map(title + " " + type);

        return new CrawledCampaign(
                source.getCode(), cpId, title, description, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, KEYWORDS_TAG
        );
    }

    private static Integer parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "dailyview-" + i,
                    "[데일리뷰] 체험단 캠페인 #" + i,
                    "데일리뷰 체험단 설명 " + i,
                    "데일리뷰 상세 내용 " + i,
                    "https://placehold.co/300x200?text=DAILYVIEW+" + i,
                    BASE_URL + "/campaign/dailyview-" + i,
                    cat, status,
                    5 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",데일리뷰,체험단"
            ));
        }
        return mocks;
    }
}
