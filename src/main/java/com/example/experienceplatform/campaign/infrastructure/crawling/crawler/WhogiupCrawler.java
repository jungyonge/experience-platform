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
public class WhogiupCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(WhogiupCrawler.class);
    private static final String BASE_URL = "https://www.whogiup.com";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    private int campaignIndex = 0;

    public WhogiupCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                           RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "WHOGIUP";
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
            log.warn("WHOGIUP robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        campaignIndex = 0;

        try {
            Document doc = jsoupClient.fetch(BASE_URL);

            // 후기업 사이트 - 메인 페이지에서 캠페인 섹션 탐색
            // 캠페인 카드 또는 리스트 아이템
            Elements items = doc.select(".campaign-card, .card, .item, a[href*=campaign]:has(img)");

            if (items.isEmpty()) {
                // Fallback: 이미지 포함 링크 탐색
                items = doc.select("a[href]:has(img)");
            }

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("WHOGIUP 아이템 파싱 실패: {}", e.getMessage());
                }
            }

            if (results.isEmpty()) {
                log.info("WHOGIUP 사이트에서 캠페인 데이터를 추출할 수 없습니다. HTML 구조가 동적 로딩 또는 인증이 필요할 수 있습니다.");
            }
        } catch (Exception e) {
            log.error("WHOGIUP 크롤링 실패: {}", e.getMessage());
        }

        log.info("WHOGIUP 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String href;
        if (item.tagName().equals("a")) {
            href = item.attr("href");
        } else {
            Element link = item.selectFirst("a[href]");
            if (link == null) return null;
            href = link.attr("href");
        }
        if (href.isEmpty() || href.equals("#") || href.equals("/")) return null;

        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        // 제목 추출
        String title = "";
        Element titleEl = item.selectFirst(".title, h3, h4, strong, .name, .campaign-title");
        if (titleEl != null) title = titleEl.text().trim();
        if (title.isEmpty()) title = item.attr("title");
        if (title.isEmpty()) {
            Element img = item.selectFirst("img");
            if (img != null) title = img.attr("alt").trim();
        }
        if (title.isEmpty() || title.length() < 3) return null;

        // 썸네일
        Element img = item.selectFirst("img");
        String thumbnailUrl = null;
        if (img != null) {
            thumbnailUrl = img.attr("src");
            if (thumbnailUrl.isEmpty()) thumbnailUrl = img.attr("data-src");
            if (thumbnailUrl != null && thumbnailUrl.startsWith("//")) {
                thumbnailUrl = "https:" + thumbnailUrl;
            } else if (thumbnailUrl != null && !thumbnailUrl.isEmpty() && !thumbnailUrl.startsWith("http")) {
                thumbnailUrl = BASE_URL + thumbnailUrl;
            }
        }

        campaignIndex++;
        String originalId = "whogiup-" + campaignIndex;

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                null, null, null, null,
                null, "블로그 리뷰 작성", null, "후기업,체험단"
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
                    source.getCode(), "whogiup-" + i,
                    "[후기업] 체험단 캠페인 #" + i,
                    "후기업 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=WHOGIUP+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",후기업,체험단"
            ));
        }
        return mocks;
    }
}
