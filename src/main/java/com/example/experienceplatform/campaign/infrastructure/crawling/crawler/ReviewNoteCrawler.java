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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class ReviewNoteCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ReviewNoteCrawler.class);
    private static final String BASE_URL = "https://www.reviewnote.co.kr";
    private static final String IMAGE_BASE = "https://img.reviewnote.co.kr/";
    private static final String[] SORT_PAGES = {"new", "popular", "premium", "nearEnd"};

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final DetailPageEnricher enricher;
    private final HttpClient httpClient;

    public ReviewNoteCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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

        List<CrawledCampaign> results = new ArrayList<>(deduped.values());
        enrichAddressFromApi(results);
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("REVIEWNOTE 크롤링 완료: {}건", results.size());
        return results;
    }

    private static final String DETAIL_API_URL = BASE_URL + "/api/campaign";

    private void enrichAddressFromApi(List<CrawledCampaign> results) {
        String fidToken = properties.getReviewnoteFidToken();
        String session = properties.getReviewnoteSession();
        if (fidToken.isEmpty() || session.isEmpty()) {
            log.debug("REVIEWNOTE 인증 토큰 미설정, 주소 enrichment 건너뜀");
            return;
        }
        String cookie = "fidToken=" + fidToken + "; reviewNoteSession=" + session;

        for (int i = 0; i < results.size(); i++) {
            CrawledCampaign c = results.get(i);
            if (c.getAddress() != null && c.getAddress().matches(".*(?:로|길|동)\\s*\\d+.*")) continue;
            try {
                Thread.sleep(50);
                String url = DETAIL_API_URL + "?id=" + c.getOriginalId();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header("Cookie", cookie)
                        .header("User-Agent", properties.getUserAgent())
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode data = root.path("data");
                    String addr1 = data.path("address1").asText("").trim();
                    String addr2 = data.path("address2").asText("").trim();
                    if (!addr1.isEmpty()) {
                        String address = addr2.isEmpty() ? addr1 : addr1 + " " + addr2;
                        results.set(i, new CrawledCampaign(
                                c.getSourceCode(), c.getOriginalId(), c.getTitle(),
                                c.getDescription(), c.getDetailContent(), c.getThumbnailUrl(), c.getOriginalUrl(),
                                c.getCategory(), c.getStatus(), c.getRecruitCount(),
                                c.getApplyStartDate(), c.getApplyEndDate(), c.getAnnouncementDate(),
                                c.getReward(), c.getMission(), address, c.getKeywords(), c.getCurrentApplicants()));
                    }
                }
            } catch (Exception e) {
                log.debug("REVIEWNOTE 주소 API 호출 실패 {}: {}", c.getOriginalId(), e.getMessage());
            }
        }
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

        for (int i = 1; i <= properties.getMockCount(); i++) {
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
