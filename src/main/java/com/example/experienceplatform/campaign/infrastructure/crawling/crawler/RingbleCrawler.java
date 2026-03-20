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
public class RingbleCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(RingbleCrawler.class);
    private static final String BASE_URL = "https://ringble.co.kr";
    private static final Pattern DDAY_PATTERN = Pattern.compile("(\\d+)일\\s*남음");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    private int campaignIndex = 0;

    public RingbleCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                          RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "RINGBLE";
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
            log.warn("RINGBLE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        campaignIndex = 0;

        // Fetch main page and deadline page
        String[] urls = {
                BASE_URL + "/",
                BASE_URL + "/html_file.php?file=category_deadline.html"
        };

        for (String url : urls) {
            try {
                Document doc = jsoupClient.fetch(url);
                List<CrawledCampaign> campaigns = parsePage(doc, source);
                results.addAll(campaigns);
                delayHandler.delay();
            } catch (Exception e) {
                log.error("RINGBLE 페이지 크롤링 실패 ({}): {}", url, e.getMessage());
            }
        }

        log.info("RINGBLE 크롤링 완료: {}건", results.size());
        return results;
    }

    private List<CrawledCampaign> parsePage(Document doc, CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();

        // Find list items with title class
        Elements titleElements = doc.select(".list_title");
        for (Element titleEl : titleElements) {
            try {
                CrawledCampaign campaign = parseByTitle(titleEl, source);
                if (campaign != null) results.add(campaign);
            } catch (Exception e) {
                log.warn("RINGBLE 아이템 파싱 실패: {}", e.getMessage());
            }
        }

        // Also try finding campaign-like links with images
        if (results.isEmpty()) {
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                Element img = link.selectFirst("img");
                if (img == null) continue;
                String href = link.attr("href");
                if (href.contains("bbs_detail") || href.contains("view")) continue;

                String title = link.attr("title");
                if (title.isEmpty()) title = img.attr("alt");
                if (title.isEmpty()) {
                    Element titleInside = link.selectFirst(".list_title, .title, strong");
                    if (titleInside != null) title = titleInside.text().trim();
                }

                if (!title.isEmpty() && title.length() > 3) {
                    try {
                        campaignIndex++;
                        String originalId = "ringble-" + campaignIndex;
                        String thumbnailUrl = img.attr("abs:src");
                        String originalUrl = href.startsWith("http") ? href : BASE_URL + "/" + href;

                        // D-day
                        LocalDate applyEndDate = null;
                        Element parent = link.parent();
                        if (parent != null) {
                            String parentText = parent.text();
                            Matcher ddayMatcher = DDAY_PATTERN.matcher(parentText);
                            if (ddayMatcher.find()) {
                                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
                            } else if (parentText.contains("오늘 마감") || parentText.contains("오늘마감")) {
                                applyEndDate = LocalDate.now();
                            }
                        }

                        CampaignCategory category = CategoryMapper.map(title);

                        results.add(new CrawledCampaign(
                                source.getCode(), originalId, title, null, null,
                                thumbnailUrl.isEmpty() ? null : thumbnailUrl,
                                originalUrl, category, CampaignStatus.RECRUITING,
                                null, null, applyEndDate, null,
                                null, "블로그 리뷰 작성", null, "링블,체험단"
                        ));
                    } catch (Exception e) {
                        log.warn("RINGBLE 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
            }
        }

        return results;
    }

    private CrawledCampaign parseByTitle(Element titleEl, CrawlingSource source) {
        String title = titleEl.text().trim();
        if (title.isEmpty() || title.length() < 3) return null;

        campaignIndex++;
        String originalId = "ringble-" + campaignIndex;

        Element parent = titleEl.parent();
        Element link = parent != null ? parent.selectFirst("a[href]") : null;
        String originalUrl = BASE_URL;
        if (link != null) {
            String href = link.attr("href");
            originalUrl = href.startsWith("http") ? href : BASE_URL + "/" + href;
        }

        Element img = parent != null ? parent.selectFirst("img") : null;
        String thumbnailUrl = img != null ? img.attr("abs:src") : null;

        LocalDate applyEndDate = null;
        if (parent != null) {
            String parentText = parent.text();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(parentText);
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
            }
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                null, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, "링블,체험단"
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
                    source.getCode(), "ringble-" + i,
                    "[링블] 체험단 캠페인 #" + i, "링블 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=RINGBLE+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",링블,체험단"
            ));
        }
        return mocks;
    }
}
