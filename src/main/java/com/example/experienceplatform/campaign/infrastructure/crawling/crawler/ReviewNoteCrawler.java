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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class ReviewNoteCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ReviewNoteCrawler.class);
    private static final String BASE_URL = "https://reviewnote.co.kr";
    private static final String IMAGE_BASE = "https://img.reviewnote.co.kr/";
    private static final String[] SORT_PAGES = {"new", "popular", "premium", "nearEnd"};

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;

    public ReviewNoteCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "REVIEWNOTE";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/campaigns")) {
            log.warn("REVIEWNOTE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        Map<String, CrawledCampaign> deduped = new LinkedHashMap<>();

        for (String sort : SORT_PAGES) {
            try {
                String url = BASE_URL + "/campaigns?s=" + sort;
                Document doc = jsoupClient.fetch(url);
                Element scriptEl = doc.selectFirst("script#__NEXT_DATA__");
                if (scriptEl == null) continue;

                JsonNode root = objectMapper.readTree(scriptEl.data());
                JsonNode pageProps = root.path("props").path("pageProps");

                for (String listKey : List.of("premiums", "populars", "nearEnds", "recents")) {
                    JsonNode list = pageProps.get(listKey);
                    if (list == null || !list.isArray()) continue;
                    for (JsonNode item : list) {
                        try {
                            CrawledCampaign campaign = parseItem(item, source);
                            if (campaign != null) {
                                deduped.putIfAbsent(campaign.getOriginalId(), campaign);
                            }
                        } catch (Exception e) {
                            log.warn("REVIEWNOTE 아이템 파싱 실패: {}", e.getMessage());
                        }
                    }
                }

                delayHandler.delay();
            } catch (Exception e) {
                log.error("REVIEWNOTE 페이지 크롤링 실패 (sort={}): {}", sort, e.getMessage());
            }
        }

        log.info("REVIEWNOTE 크롤링 완료: {}건", deduped.size());
        return new ArrayList<>(deduped.values());
    }

    public CrawledCampaign parseItem(JsonNode item, CrawlingSource source) {
        long id = item.path("id").asLong(0);
        if (id == 0) return null;
        String originalId = String.valueOf(id);

        String title = item.path("title").asText("").trim();
        if (title.isEmpty()) return null;

        String imageKey = item.path("imageKey").asText(null);
        String thumbnailUrl = imageKey != null ? IMAGE_BASE + imageKey : null;
        String originalUrl = BASE_URL + "/campaigns/" + originalId;

        String offer = item.path("offer").asText(null);
        String channel = item.path("channel").asText("");
        String sortType = item.path("sort").asText("");
        String city = item.path("city").asText("");
        String sidoName = item.path("sido").path("name").asText("");
        String categoryTitle = item.path("category").path("title").asText("");

        Integer recruitCount = item.has("infNum") ? item.get("infNum").asInt() : null;

        LocalDate applyEndDate = parseIsoDate(item.path("applyEndAt").asText(null));
        LocalDate reviewEndDate = parseIsoDate(item.path("reviewEndAt").asText(null));

        CampaignCategory category = CategoryMapper.map(categoryTitle);
        CampaignStatus status = parseStatus(item.path("status").asText(""));

        String address = buildAddress(city, sidoName);
        String mission = buildMission(channel, sortType);
        String keywords = buildKeywords(channel, categoryTitle, sortType);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                offer, mission, address, keywords
        );
    }

    private CampaignStatus parseStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SELECT", "APPLY", "REVIEW", "POSTING" -> CampaignStatus.RECRUITING;
            default -> CampaignStatus.CLOSED;
        };
    }

    private LocalDate parseIsoDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        try {
            return Instant.parse(isoDate).atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAddress(String city, String sidoName) {
        if (city.isEmpty() && sidoName.isEmpty()) return null;
        if (sidoName.isEmpty()) return city;
        return city + " " + sidoName;
    }

    private String buildMission(String channel, String sortType) {
        String media = channel.isEmpty() ? "블로그" : channel.toLowerCase();
        String type = sortType.equalsIgnoreCase("VISIT") ? "방문 체험 후 " : "";
        return type + media + " 리뷰 작성";
    }

    private String buildKeywords(String channel, String categoryTitle, String sortType) {
        StringJoiner joiner = new StringJoiner(",");
        if (!categoryTitle.isEmpty()) joiner.add(categoryTitle);
        if (!channel.isEmpty()) joiner.add(channel.toLowerCase());
        if (!sortType.isEmpty()) joiner.add(sortType.equalsIgnoreCase("VISIT") ? "방문형" : "배송형");
        joiner.add("체험단");
        return joiner.toString();
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.TRAVEL,
                CampaignCategory.LIFE, CampaignCategory.DIGITAL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "reviewnote-" + i,
                    "[리뷰노트] 체험단 캠페인 #" + i, "리뷰노트 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=REVIEWNOTE+" + i,
                    BASE_URL + "/campaigns/" + (1139300 + i),
                    cat, status, 5 + i % 8, today.minusDays(3), today.plusDays(7 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",리뷰노트,체험단"
            ));
        }
        return mocks;
    }
}
