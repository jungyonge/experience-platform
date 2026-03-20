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
public class TbleCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(TbleCrawler.class);
    private static final String BASE_URL = "https://tble.kr";
    private static final String LIST_URL = BASE_URL + "/category.php?type=l";
    private static final Pattern ID_PATTERN = Pattern.compile("cp_id=([^&]+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public TbleCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                       RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "TBLE";
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
            log.warn("TBLE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(LIST_URL);
            Elements items = doc.select("div.campain_list > div.item");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("TBLE 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("TBLE 페이지 크롤링 실패: {}", e.getMessage());
        }

        log.info("TBLE 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title
        Element titleEl = item.selectFirst("div.info div.t2");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // ID from a.link or a[href*=view.php] href
        Element link = item.selectFirst("a.link, a[href*=view.php]");
        if (link == null) return null;
        String href = link.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Original URL
        String originalUrl = BASE_URL + "/campaign/view?cp_id=" + originalId;

        // Thumbnail
        String thumbnailUrl = null;
        Element img = item.selectFirst("div.img > a.link > img, div.img > a > img");
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + "/" + src;
            }
        }

        // D-day
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Element ddayEl = item.selectFirst("div.info div.t1 span.ps_remain");
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

        // Recruit count from div.t4 "신청 N 명 / 모집 M명"
        Integer recruitCount = null;
        Element recruitEl = item.selectFirst("div.t4");
        if (recruitEl != null) {
            Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        // Reward
        String reward = null;
        Element rewardEl = item.selectFirst("div.t3");
        if (rewardEl != null) {
            reward = rewardEl.text().trim();
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "티블,체험단"
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
                    source.getCode(), "tble-" + i,
                    "[티블] 체험단 캠페인 #" + i,
                    "티블 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=TBLE+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",티블,체험단"
            ));
        }
        return mocks;
    }
}
