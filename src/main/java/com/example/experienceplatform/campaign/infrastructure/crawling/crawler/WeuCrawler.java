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

@Component
public class WeuCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(WeuCrawler.class);
    private static final String BASE_URL = "https://weu.kr";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public WeuCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                      RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "WEU";
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
            log.warn("WEU robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        int itemIndex = 0;

        try {
            Document doc = jsoupClient.fetch(BASE_URL);

            // WEU는 SPA 기반으로 초기 HTML에 캠페인 데이터가 제한적임
            // og:image 메타 태그와 기본 구조만 포함됨
            // 캠페인 카드 탐색 시도
            Elements items = doc.select(".campaign-card, .card, .item, a[href*=campaign]:has(img)");

            if (items.isEmpty()) {
                // Fallback: 이미지가 포함된 링크 탐색
                items = doc.select("a[href]:has(img)");
            }

            for (Element item : items) {
                try {
                    String href = item.tagName().equals("a") ? item.attr("href") : "";
                    if (!item.tagName().equals("a")) {
                        Element link = item.selectFirst("a[href]");
                        if (link != null) href = link.attr("href");
                    }
                    if (href.isEmpty() || href.equals("#") || href.equals("/")) continue;

                    String title = "";
                    Element titleEl = item.selectFirst(".title, h3, h4, strong, .name");
                    if (titleEl != null) title = titleEl.text().trim();
                    if (title.isEmpty()) title = item.attr("title");
                    if (title.isEmpty()) {
                        Element img = item.selectFirst("img");
                        if (img != null) title = img.attr("alt").trim();
                    }
                    if (title.isEmpty() || title.length() < 3) continue;

                    itemIndex++;
                    String originalId = "weu-" + itemIndex;
                    String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

                    Element img = item.selectFirst("img");
                    String thumbnailUrl = null;
                    if (img != null) {
                        thumbnailUrl = img.attr("src");
                        if (thumbnailUrl.startsWith("//")) thumbnailUrl = "https:" + thumbnailUrl;
                        else if (!thumbnailUrl.startsWith("http")) thumbnailUrl = BASE_URL + thumbnailUrl;
                    }

                    CampaignCategory category = CategoryMapper.map(title);

                    results.add(new CrawledCampaign(
                            source.getCode(), originalId, title, null, null,
                            thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                            null, null, null, null,
                            null, "블로그 리뷰 작성", null, "위유,체험단"
                    ));
                } catch (Exception e) {
                    log.warn("WEU 아이템 파싱 실패: {}", e.getMessage());
                }
            }

            if (results.isEmpty()) {
                log.info("WEU 사이트는 SPA 기반으로 Jsoup 크롤링으로 캠페인 데이터를 추출할 수 없습니다.");
            }
        } catch (Exception e) {
            log.error("WEU 크롤링 실패: {}", e.getMessage());
        }

        log.info("WEU 크롤링 완료: {}건", results.size());
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
                    source.getCode(), "weu-" + i,
                    "[위유] 체험단 캠페인 #" + i,
                    "위유 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=WEU+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",위유,체험단"
            ));
        }
        return mocks;
    }
}
