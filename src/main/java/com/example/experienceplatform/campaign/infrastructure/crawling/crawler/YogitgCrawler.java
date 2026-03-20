package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class YogitgCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(YogitgCrawler.class);
    private static final String BASE_URL = "https://yogitg.co.kr";
    private static final Pattern DAYS_LEFT_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final String KEYWORDS_TAG = "요깃지,체험단";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public YogitgCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                         RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler,
                         ObjectMapper objectMapper) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getCrawlerType() {
        return "YOGITG";
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

                // last_page == 1 means this is the last page
                if (root.path("last_page").asInt(0) == 1) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("{} 페이지 {} 크롤링 실패: {}", getCrawlerType(), page, e.getMessage());
                break;
            }
        }
        log.info("{} 크롤링 완료: {}건", getCrawlerType(), results.size());
        return results;
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
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.FOOD, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 10; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "yogitg-" + i,
                    "[요깃지] 체험단 캠페인 #" + i, "요깃지 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=YOGITG+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",요깃지,체험단"
            ));
        }
        return mocks;
    }
}
