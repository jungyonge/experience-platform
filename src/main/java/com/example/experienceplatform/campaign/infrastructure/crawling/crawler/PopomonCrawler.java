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
public class PopomonCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(PopomonCrawler.class);
    private static final String BASE_URL = "https://popomon.com";
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public PopomonCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                           RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "POPOMON";
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
            log.warn("POPOMON robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        int itemIndex = 0;

        try {
            // Next.js 기반 SPA - Jsoup으로 SSR된 HTML 탐색
            Document doc = jsoupClient.fetch(BASE_URL + "/campaign");

            // 캠페인 카드 링크 탐색 - Tailwind CSS 기반 레이아웃
            Elements cards = doc.select("a[href*=/campaign/]");

            if (cards.isEmpty()) {
                // Fallback: section 내부의 캠페인 아이템
                cards = doc.select("section a[href]:has(img)");
            }

            for (Element card : cards) {
                try {
                    String href = card.attr("href");
                    if (href.isEmpty() || href.equals("/campaign") || href.equals("/campaign/")) continue;

                    // 캠페인 ID 추출
                    String originalId = href.replaceAll(".*/(\\d+).*", "$1");
                    if (originalId.equals(href) || originalId.isEmpty()) {
                        itemIndex++;
                        originalId = "popomon-" + itemIndex;
                    }

                    // 제목 추출
                    String title = card.text().trim();
                    if (title.isEmpty() || title.length() < 3) continue;
                    // 너무 긴 텍스트는 제목이 아닐 수 있음
                    if (title.length() > 200) {
                        Element titleEl = card.selectFirst("h3, h4, p, strong, .title");
                        if (titleEl != null) title = titleEl.text().trim();
                    }
                    if (title.isEmpty() || title.length() < 3) continue;

                    String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

                    // 썸네일
                    String thumbnailUrl = null;
                    Element img = card.selectFirst("img");
                    if (img != null) {
                        thumbnailUrl = img.attr("src");
                        if (thumbnailUrl.isEmpty()) thumbnailUrl = img.attr("data-src");
                        if (thumbnailUrl != null && thumbnailUrl.startsWith("/")) {
                            thumbnailUrl = BASE_URL + thumbnailUrl;
                        }
                    }

                    CampaignCategory category = CategoryMapper.map(title);

                    results.add(new CrawledCampaign(
                            source.getCode(), originalId, title, null, null,
                            thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                            null, null, null, null,
                            null, "블로그 리뷰 작성", null, "포포몬,체험단"
                    ));
                } catch (Exception e) {
                    log.warn("POPOMON 아이템 파싱 실패: {}", e.getMessage());
                }
            }

            if (results.isEmpty()) {
                log.info("POPOMON 사이트는 Next.js SPA로 Jsoup 크롤링으로 캠페인 데이터가 제한적입니다.");
            }
        } catch (Exception e) {
            log.error("POPOMON 크롤링 실패: {}", e.getMessage());
        }

        log.info("POPOMON 크롤링 완료: {}건", results.size());
        return results;
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
                    source.getCode(), "popomon-" + i,
                    "[포포몬] 체험단 캠페인 #" + i,
                    "포포몬 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=POPOMON+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",포포몬,체험단"
            ));
        }
        return mocks;
    }
}
