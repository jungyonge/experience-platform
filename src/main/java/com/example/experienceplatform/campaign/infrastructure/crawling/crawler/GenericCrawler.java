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

import java.util.ArrayList;
import java.util.List;

@Component
public class GenericCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(GenericCrawler.class);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public GenericCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                          RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "GENERIC";
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
        String listUrlPattern = source.getListUrlPattern();

        if (listUrlPattern == null || listUrlPattern.isBlank()) {
            log.warn("[{}] listUrlPattern이 설정되지 않아 크롤링을 건너뜁니다.", source.getCode());
            return List.of();
        }

        if (!robotsTxtChecker.isAllowed(baseUrl, "/")) {
            log.warn("[{}] robots.txt에 의해 크롤링이 차단되었습니다.", source.getCode());
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = listUrlPattern.replace("{page}", String.valueOf(page));
                Document doc = jsoupClient.fetch(url);
                Elements links = doc.select("a[href]");
                if (links.isEmpty()) break;

                int count = 0;
                for (Element link : links) {
                    try {
                        String title = link.text().trim();
                        String href = link.attr("abs:href");
                        if (title.isEmpty() || href.isEmpty() || title.length() < 5) continue;

                        String originalId = extractId(href);
                        if (originalId == null) continue;

                        String thumbnailUrl = null;
                        Element img = link.selectFirst("img");
                        if (img != null) {
                            thumbnailUrl = img.attr("abs:src");
                        }

                        results.add(new CrawledCampaign(
                                source.getCode(), originalId, title, null, null,
                                thumbnailUrl, href,
                                CampaignCategory.ETC, CampaignStatus.RECRUITING,
                                null, null, null, null,
                                null, null, null, null
                        ));
                        count++;
                    } catch (Exception e) {
                        log.warn("[{}] 아이템 파싱 실패: {}", source.getCode(), e.getMessage());
                    }
                }
                if (count == 0) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("[{}] 페이지 {} 크롤링 실패: {}", source.getCode(), page, e.getMessage());
                break;
            }
        }
        return results;
    }

    private String extractId(String url) {
        if (url == null || url.isEmpty()) return null;
        String[] parts = url.split("[/?#]");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                return parts[i];
            }
        }
        return null;
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            mocks.add(new CrawledCampaign(
                    source.getCode(), "generic-" + source.getCode().toLowerCase() + "-" + i,
                    "[" + source.getName() + "] 체험단 캠페인 #" + i,
                    source.getName() + " 체험단 설명 " + i,
                    null, null,
                    source.getBaseUrl() + "/campaign/generic-" + i,
                    CampaignCategory.ETC, CampaignStatus.RECRUITING,
                    5, null, null, null,
                    null, null, null, null
            ));
        }
        return mocks;
    }
}
