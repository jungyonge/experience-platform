package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import com.fasterxml.jackson.databind.JsonNode;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

/**
 * 뷰티의여왕 크롤러 - 디너의여왕 자매 사이트로 동일한 API 패턴 사용
 */
@Component
public class BeautyQueenCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(BeautyQueenCrawler.class);
    private static final String BASE_URL = "https://bqueens.net";
    private static final String API_URL = "https://bqueens.net/taste/taste_list";
    private static final Pattern ID_PATTERN = Pattern.compile("/taste/(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");

    private final CrawlingProperties properties;
    private final CrawlingDelayHandler delayHandler;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final DetailPageEnricher enricher;

    public BeautyQueenCrawler(CrawlingProperties properties, CrawlingDelayHandler delayHandler,
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
        return "BEAUTY_QUEEN";
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
                String body = "ct=&area1=&area2=&page=" + page;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("User-Agent", properties.getUserAgent())
                        .header("Referer", BASE_URL + "/taste/tastes")
                        .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("BEAUTY_QUEEN API 응답 오류: HTTP {}", response.statusCode());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());
                String layout = root.path("layout").asText("");
                boolean hasNext = root.path("has_next").asBoolean(false);

                if (layout.isEmpty()) break;

                Document doc = Jsoup.parse(layout);
                Elements cards = doc.select("a.item-content[href*=/taste/]");
                if (cards.isEmpty()) {
                    cards = doc.select("a[href*=/taste/]");
                }

                for (Element card : cards) {
                    try {
                        CrawledCampaign campaign = parseItem(card, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("BEAUTY_QUEEN 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                if (!hasNext) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("BEAUTY_QUEEN 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("BEAUTY_QUEEN 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc == null) metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        Integer currentApplicants = null;
        Matcher m = Pattern.compile("신청\\s*(\\d+)").matcher(doc.text());
        if (m.find()) currentApplicants = Integer.parseInt(m.group(1));

        Integer recruitCount = null;
        Matcher rm = Pattern.compile("모집\\s*(\\d+)").matcher(doc.text());
        if (rm.find()) recruitCount = Integer.parseInt(rm.group(1));

        String reward = null;
        for (Element el : doc.select("th, dt, .label")) {
            if (el.text().contains("제공") || el.text().contains("혜택")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    reward = sibling.text().trim();
                    break;
                }
            }
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                coalesce(campaign.getRecruitCount(), recruitCount), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    public CrawledCampaign parseItem(Element card, CrawlingSource source) {
        String href = card.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        // bqueens.net: title from img alt or aside h5.ellipsis
        Element img = card.selectFirst("img");
        String thumbnailUrl = img != null ? img.attr("src") : null;

        String title = img != null ? img.attr("alt").trim() : "";
        if (title.isEmpty()) {
            title = card.attr("title").trim();
        }
        if (title.isEmpty()) {
            title = card.text().trim();
        }
        if (title.isEmpty()) return null;

        // D-day from parent div.item > aside > h5 > b
        LocalDate applyEndDate = null;
        Element parentItem = card.parent();
        if (parentItem != null) {
            String parentText = parentItem.text();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(parentText);
            if (ddayMatcher.find()) {
                int daysLeft = Integer.parseInt(ddayMatcher.group(1));
                applyEndDate = LocalDate.now().plusDays(daysLeft);
            }
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                null, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, "뷰티의여왕,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.BEAUTY, CampaignCategory.FOOD,
                CampaignCategory.LIFE, CampaignCategory.DIGITAL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "beautyqueen-" + i,
                    "[뷰티의여왕] 체험단 캠페인 #" + i,
                    "뷰티의여왕 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=BEAUTY_QUEEN+" + i,
                    BASE_URL + "/taste/" + (100000 + i),
                    cat, status,
                    3 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",뷰티의여왕,체험단"
            ));
        }
        return mocks;
    }
}
