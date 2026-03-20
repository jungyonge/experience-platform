package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import org.jsoup.Jsoup;
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
public class OpenreviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(OpenreviewCrawler.class);
    private static final String BASE_URL = "https://openreview.kr";
    private static final String AJAX_URL = "https://openreview.kr/main/m/ajax/ajax.exp.list.php";
    private static final Pattern ID_PATTERN = Pattern.compile("ex_idx=(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public OpenreviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                             RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "OPENREVIEW";
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
            log.warn("OPENREVIEW robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        int maxPages = properties.getMaxPagesPerSite();

        for (int page = 0; page < maxPages; page++) {
            try {
                if (page > 0) {
                    delayHandler.delay();
                }
                int offset = page * 20;
                Document doc = fetchAjaxPage(offset);
                List<CrawledCampaign> pageCampaigns = parseCampaignItems(doc, source);
                if (pageCampaigns.isEmpty()) break;
                results.addAll(pageCampaigns);
            } catch (Exception e) {
                log.error("OPENREVIEW 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        log.info("OPENREVIEW 크롤링 완료: {}건", results.size());
        return results;
    }

    private Document fetchAjaxPage(int offset) {
        try {
            return Jsoup.connect(AJAX_URL)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getConnectionTimeoutMs())
                    .referrer(BASE_URL)
                    .data("start", String.valueOf(offset))
                    .data("list", "20")
                    .data("ex_channel", "")
                    .data("ex_adver_type", "")
                    .data("ex_type", "")
                    .data("cate_idx", "")
                    .data("sst", "close")
                    .data("sod", "desc")
                    .data("stx", "")
                    .post();
        } catch (Exception e) {
            throw new CrawlingException("OPENREVIEW AJAX 페이지 조회 실패: offset=" + offset, e);
        }
    }

    private List<CrawledCampaign> parseCampaignItems(Document doc, CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();
        Elements items = doc.select("li:has(.vertical-card)");

        for (Element item : items) {
            try {
                CrawledCampaign campaign = parseItem(item, source);
                if (campaign != null) results.add(campaign);
            } catch (Exception e) {
                log.warn("OPENREVIEW 아이템 파싱 실패: {}", e.getMessage());
            }
        }
        return results;
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title
        Element titleEl = item.selectFirst(".card-title p");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // ID from campaign-img link href ex_idx=(\d+)
        Element link = item.selectFirst("a.campaign-img, a[href*=ex_idx]");
        if (link == null) return null;
        String href = link.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Original URL
        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        // Thumbnail
        String thumbnailUrl = null;
        Element img = item.selectFirst(".campaign-image a img");
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
            }
        }

        // D-day
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Element ddayEl = item.selectFirst(".dday-area p");
        if (ddayEl != null) {
            String ddayText = ddayEl.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(ddayText);
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
            }
            if (ddayText.contains("마감") || ddayText.contains("종료")) {
                status = CampaignStatus.CLOSED;
            }
        }

        // Recruit count
        Integer recruitCount = null;
        Element recruitEl = item.selectFirst(".apply-limit span");
        if (recruitEl != null) {
            Matcher recruitMatcher = NUMBER_PATTERN.matcher(recruitEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        // Reward
        String reward = null;
        Element rewardEl = item.selectFirst(".card-text p");
        if (rewardEl != null) {
            reward = rewardEl.text().trim();
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "오픈리뷰,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 10; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "openreview-" + i,
                    "[오픈리뷰] 체험단 캠페인 #" + i,
                    "오픈리뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=OPENREVIEW+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",오픈리뷰,체험단"
            ));
        }
        return mocks;
    }
}
