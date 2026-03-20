package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OdiyaCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(OdiyaCrawler.class);
    private static final String BASE_URL = "https://odiya.kr";
    private static final String LIST_URL = BASE_URL + "/category.php?category=829";
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern ID_PATTERN = Pattern.compile("number=(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");
    private static final Pattern APPLY_PATTERN = Pattern.compile("신청\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public OdiyaCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                        RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "ODIYA";
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
            log.warn("ODIYA robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(LIST_URL);
            Elements items = doc.select("div.rows_margin");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("ODIYA 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("ODIYA 페이지 크롤링 실패: {}", e.getMessage());
        }

        log.info("ODIYA 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title from td.font_17 strong
        Element titleEl = item.selectFirst("td.font_17 strong");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // ID from table[onclick*=detail.php] onclick attribute
        String originalId = null;
        Element onclickEl = item.selectFirst("table[onclick*=detail.php]");
        if (onclickEl != null) {
            String onclick = onclickEl.attr("onclick");
            Matcher idMatcher = ID_PATTERN.matcher(onclick);
            if (idMatcher.find()) {
                originalId = idMatcher.group(1);
            }
        }
        if (originalId == null) return null;

        // Original URL
        String originalUrl = BASE_URL + "/detail.php?number=" + originalId + "&category=829";

        // Thumbnail from img[src*=mallimg]
        String thumbnailUrl = null;
        Element img = item.selectFirst("img[src*=mallimg]");
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                if (src.startsWith("./")) {
                    thumbnailUrl = BASE_URL + "/" + src.substring(2);
                } else if (src.startsWith("http")) {
                    thumbnailUrl = src;
                } else {
                    thumbnailUrl = BASE_URL + "/" + src;
                }
            }
        }

        // D-day from div.font_17 with D-N pattern
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Elements divs = item.select("div.font_17");
        for (Element div : divs) {
            String text = div.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(text);
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
                break;
            }
            if (text.contains("마감") || text.contains("종료")) {
                status = CampaignStatus.CLOSED;
            }
        }

        // Recruit count from strong text containing "모집"
        Integer recruitCount = null;
        Elements strongs = item.select("strong");
        for (Element strong : strongs) {
            String text = strong.text();
            if (text.contains("모집")) {
                Matcher recruitMatcher = RECRUIT_PATTERN.matcher(text);
                if (recruitMatcher.find()) {
                    recruitCount = Integer.parseInt(recruitMatcher.group(1));
                }
            }
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, "어디야,체험단"
        );
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
                    source.getCode(), "odiya-" + i,
                    "[어디야] 체험단 캠페인 #" + i, "어디야 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=ODIYA+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",어디야,체험단"
            ));
        }
        return mocks;
    }
}
