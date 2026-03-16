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
public class MbleCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(MbleCrawler.class);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public MbleCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                       RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "MBLE";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        String baseUrl = source.getBaseUrl();

        if (!robotsTxtChecker.isAllowed(baseUrl, "/campaign")) {
            log.warn("MBLE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = buildPageUrl(source, page);
                Document doc = jsoupClient.fetch(url);
                Elements items = doc.select(".campaign-card, .card, article");
                if (items.isEmpty()) break;

                for (Element item : items) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("MBLE 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("MBLE 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }
        return results;
    }

    private String buildPageUrl(CrawlingSource source, int page) {
        if (source.getListUrlPattern() != null && !source.getListUrlPattern().isBlank()) {
            return source.getListUrlPattern().replace("{page}", String.valueOf(page));
        }
        return source.getBaseUrl() + "/campaign?page=" + page;
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String baseUrl = source.getBaseUrl();
        String title = item.select("h3, .title, .name").text().trim();
        String originalId = item.select("a[href]").attr("href").replaceAll(".*/(\\d+).*", "$1");
        String originalUrl = item.select("a[href]").attr("abs:href");
        if (title.isEmpty() || originalId.isEmpty()) return null;

        String thumbnailUrl = item.select("img").attr("abs:src");
        String description = item.select(".description, .desc, p").text().trim();
        String categoryText = item.select(".category, .badge").text();
        String recruitText = item.select(".recruit, .number").text();
        String dateText = item.select(".date, .deadline").text();

        return new CrawledCampaign(
                source.getCode(), originalId, title, description, null,
                thumbnailUrl.isEmpty() ? null : thumbnailUrl,
                originalUrl.isEmpty() ? baseUrl + "/campaign/" + originalId : originalUrl,
                CategoryMapper.map(categoryText), CampaignStatus.RECRUITING,
                CrawlingNumberParser.parse(recruitText),
                null, CrawlingDateParser.parse(dateText), null,
                null, null, null, null
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.FOOD, CampaignCategory.DIGITAL,
                CampaignCategory.CULTURE, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "mble-" + i,
                    "[MBLE] 체험단 캠페인 #" + i,
                    "미블 체험단 설명 " + i,
                    "미블 상세 내용 " + i,
                    "https://placehold.co/300x200?text=MBLE+" + i,
                    source.getBaseUrl() + "/campaign/mble-" + i,
                    cat, status,
                    5 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    "인스타그램 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",미블체험단"
            ));
        }
        return mocks;
    }
}
