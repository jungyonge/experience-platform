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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Component
public class ReveCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ReveCrawler.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CAMPAIGN_BASE_URL = "https://www.revu.net/campaign/";

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ReveTokenManager tokenManager;

    public ReveCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
                       ObjectMapper objectMapper, ReveTokenManager tokenManager) {
        this.properties = properties;
        this.delayHandler = delayHandler;
        this.objectMapper = objectMapper;
        this.tokenManager = tokenManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getCrawlerType() {
        return "REVE";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        String apiToken = tokenManager.getToken();
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("REVE API 토큰을 가져올 수 없습니다. reve-api-token 또는 reve-username/reve-password 설정을 확인하세요.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = buildPageUrl(source, page);
                String json = fetchJsonWithRetry(url);
                JsonNode root = objectMapper.readTree(json);

                JsonNode items = root.get("items");
                if (items == null || !items.isArray() || items.isEmpty()) break;

                for (JsonNode item : items) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("REVE 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                int total = root.path("total").asInt(0);
                int limit = root.path("limit").asInt(35);
                int totalPages = (total + limit - 1) / limit;
                if (page >= totalPages) break;

                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("REVE 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        log.info("REVE 크롤링 완료: {}건", results.size());
        return results;
    }

    private String buildPageUrl(CrawlingSource source, int page) {
        if (source.getListUrlPattern() != null && !source.getListUrlPattern().isBlank()) {
            return source.getListUrlPattern().replace("{page}", String.valueOf(page));
        }
        return source.getBaseUrl() + "/v1/campaigns?limit=35&page=" + page + "&sort=latest&type=play";
    }

    private String fetchJsonWithRetry(String url) {
        String token = tokenManager.getToken();
        HttpResponse<String> response = doFetch(url, token);

        if (response.statusCode() == 401) {
            log.info("REVE API 토큰 만료, 갱신 시도...");
            tokenManager.invalidateToken();
            String newToken = tokenManager.refreshToken();
            if (newToken == null) {
                throw new CrawlingException("REVE API 토큰 갱신 실패");
            }
            response = doFetch(url, newToken);
        }

        if (response.statusCode() != 200) {
            throw new CrawlingException("REVE API 응답 오류: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private HttpResponse<String> doFetch(String url, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", properties.getUserAgent())
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .GET()
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new CrawlingException("REVE API 호출 실패: " + url, e);
        }
    }

    public CrawledCampaign parseItem(JsonNode item, CrawlingSource source) {
        String originalId = String.valueOf(item.path("id").asLong());
        String title = item.path("item").asText(null);
        if (title == null || title.isBlank()) {
            title = item.path("title").asText(null);
        }
        if (originalId.equals("0") || title == null || title.isBlank()) return null;

        String description = item.path("brief").asText("").trim();
        if (description.isEmpty()) description = null;

        String thumbnailUrl = item.path("thumbnail").asText(null);
        String contentImage = item.path("contentImage").asText(null);
        String originalUrl = CAMPAIGN_BASE_URL + item.path("hash").asText(originalId);

        // 주소
        String address = parseAddress(item);

        // 카테고리
        CampaignCategory category = parseCategory(item);

        // 상태
        CampaignStatus status = parseStatus(item.path("status").asText(""));

        // 모집 인원
        Integer recruitCount = item.has("reviewerLimit") ? item.get("reviewerLimit").asInt() : null;

        // 날짜
        LocalDate applyStartDate = parseDate(item.path("requestStartedOn").asText(null));
        LocalDate applyEndDate = parseDate(item.path("requestEndedOn").asText(null));
        LocalDate announcementDate = parseDate(item.path("entryAnnouncedOn").asText(null));

        // 리워드
        String reward = null;
        JsonNode campaignData = item.get("campaignData");
        if (campaignData != null) {
            reward = campaignData.path("reward").asText(null);
        }

        // 미디어 타입
        String media = item.path("media").asText("");

        // 키워드: category + localTag + media
        String keywords = buildKeywords(item, media);

        // 미션: 포스팅 기간 정보
        String mission = buildMission(item, media);

        return new CrawledCampaign(
                source.getCode(), originalId, title, description,
                contentImage != null ? "<img src=\"" + contentImage + "\">" : null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, applyStartDate, applyEndDate, announcementDate,
                reward, mission, address, keywords
        );
    }

    private String parseAddress(JsonNode item) {
        JsonNode venue = item.get("venue");
        if (venue == null) return null;

        String addressFirst = venue.path("addressFirst").asText("").trim();
        String addressLast = venue.path("addressLast").asText("").trim();

        if (addressFirst.isEmpty()) return null;
        return addressLast.isEmpty() ? addressFirst : addressFirst + " " + addressLast;
    }

    private CampaignCategory parseCategory(JsonNode item) {
        JsonNode categoryArray = item.get("category");
        if (categoryArray != null && categoryArray.isArray()) {
            for (JsonNode cat : categoryArray) {
                String catText = cat.asText("");
                CampaignCategory mapped = CategoryMapper.map(catText);
                if (mapped != CampaignCategory.ETC) return mapped;
            }
            if (categoryArray.size() > 0) {
                return CategoryMapper.map(categoryArray.get(0).asText(""));
            }
        }

        JsonNode venue = item.get("venue");
        if (venue != null) {
            String venueCategory = venue.path("category").asText("");
            return CategoryMapper.map(venueCategory);
        }

        return CampaignCategory.ETC;
    }

    private CampaignStatus parseStatus(String statusText) {
        if (statusText == null) return CampaignStatus.RECRUITING;
        return switch (statusText.toUpperCase()) {
            case "REQUEST", "DESIGN", "DESIGN_END" -> CampaignStatus.RECRUITING;
            default -> CampaignStatus.CLOSED;
        };
    }

    private LocalDate parseDate(String dateText) {
        if (dateText == null || dateText.isBlank()) return null;
        try {
            return LocalDate.parse(dateText.substring(0, 10), DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildKeywords(JsonNode item, String media) {
        StringJoiner joiner = new StringJoiner(",");

        JsonNode categoryArray = item.get("category");
        if (categoryArray != null && categoryArray.isArray()) {
            for (JsonNode cat : categoryArray) {
                String text = cat.asText("").trim();
                if (!text.isEmpty()) joiner.add(text);
            }
        }

        JsonNode localTag = item.get("localTag");
        if (localTag != null && localTag.isArray()) {
            for (JsonNode tag : localTag) {
                String text = tag.asText("").trim();
                if (!text.isEmpty()) joiner.add(text);
            }
        }

        if (!media.isEmpty()) joiner.add(media);
        joiner.add("체험단");

        return joiner.toString();
    }

    private String buildMission(JsonNode item, String media) {
        String postingStart = item.path("postingStartedOn").asText("");
        String postingEnd = item.path("postingEndedOn").asText("");
        int requiredPostCount = item.path("requiredPostCount").asInt(1);

        StringBuilder sb = new StringBuilder();
        sb.append(media.isEmpty() ? "콘텐츠" : media).append(" 리뷰 작성");
        if (requiredPostCount > 1) {
            sb.append(" (").append(requiredPostCount).append("건)");
        }
        if (!postingStart.isEmpty() && !postingEnd.isEmpty()) {
            sb.append(" / 포스팅 기간: ").append(postingStart).append(" ~ ").append(postingEnd);
        }
        return sb.toString();
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.TRAVEL,
                CampaignCategory.LIFE, CampaignCategory.DIGITAL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 12 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "reve-" + i,
                    "[REVE] 체험단 캠페인 #" + i,
                    "레뷰 체험단 설명 " + i,
                    "상세 설명 내용 " + i,
                    "https://placehold.co/300x200?text=REVE+" + i,
                    "https://www.revu.net/campaign/reve-" + i,
                    cat, status,
                    3 + i % 10,
                    today.minusDays(5),
                    today.plusDays(10 + i),
                    today.plusDays(15 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    i % 3 == 0 ? "서울 강남구 역삼동 " + i : null,
                    cat.getDisplayName() + ",체험단,리뷰"
            ));
        }
        return mocks;
    }
}
