package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;
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
public class CloudreviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(CloudreviewCrawler.class);
    private static final String BASE_URL = "https://www.cloudreview.co.kr";
    private static final String LIST_URL = BASE_URL + "/campaign/blog";
    private static final Pattern ID_PATTERN = Pattern.compile("/campaign/detail/(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("(\\d+)인\\s*모집");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public CloudreviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                              RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler,
                              DetailPageEnricher enricher) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
        this.enricher = enricher;
    }

    @Override
    public String getCrawlerType() {
        return "CLOUDREVIEW";
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
            log.warn("CLOUDREVIEW robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(LIST_URL);
            Elements items = doc.select("#campaign-lists > div.col-span-1");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("CLOUDREVIEW 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("CLOUDREVIEW 페이지 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("CLOUDREVIEW 크롤링 완료: {}건", results.size());
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

        String reward = null;
        for (Element el : doc.select("th, dt, .label, .font-bold")) {
            if (el.text().contains("제공") || el.text().contains("리워드")) {
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
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title from div.text-sm.px-3.pt-3.tracking-tighter.truncate > a
        Element titleEl = item.selectFirst("div.text-sm.px-3.pt-3.tracking-tighter.truncate > a");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // ID from campaign link href /campaign/detail/{id}
        String href = titleEl.attr("href");
        if (href.isEmpty()) {
            // Try other links in the item
            Element altLink = item.selectFirst("a[href*=/campaign/detail/]");
            if (altLink != null) href = altLink.attr("href");
        }
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Original URL
        String originalUrl = BASE_URL + "/campaign/detail/" + originalId;

        // Thumbnail from img.lazy data-original attribute
        String thumbnailUrl = null;
        Element img = item.selectFirst("img.lazy");
        if (img != null) {
            String src = img.attr("data-original");
            if (src.isEmpty()) {
                src = img.attr("src");
            }
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
            }
        }

        // D-day from div.px-2.pb-3 > span containing "남음"
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Elements spans = item.select("div.px-2.pb-3 > span");
        for (Element span : spans) {
            String text = span.text().trim();
            if (text.contains("남음")) {
                Matcher ddayMatcher = DDAY_PATTERN.matcher(text);
                if (ddayMatcher.find()) {
                    applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
                }
            }
            if (text.contains("마감") || text.contains("종료")) {
                status = CampaignStatus.CLOSED;
            }
        }

        // Recruit count from div.text-xs.px-3.pt-1 (e.g., "3인 모집")
        Integer recruitCount = null;
        Element recruitEl = item.selectFirst("div.text-xs.px-3.pt-1");
        if (recruitEl != null) {
            Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        // Tags from div.text-sm.px-3.pt-1.pb-1 > span
        String keywords = "클라우드리뷰,체험단";
        Elements tagEls = item.select("div.text-sm.px-3.pt-1.pb-1 > span");
        if (!tagEls.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Element tag : tagEls) {
                String tagText = tag.text().trim().replace("#", "");
                if (!tagText.isEmpty()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(tagText);
                }
            }
            if (sb.length() > 0) {
                keywords = sb + ",클라우드리뷰,체험단";
            }
        }

        CampaignCategory category = CategoryMapper.map(title + " " + keywords);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, keywords
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
                    source.getCode(), "cloudreview-" + i,
                    "[클라우드리뷰] 체험단 캠페인 #" + i,
                    "클라우드리뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=CLOUDREVIEW+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",클라우드리뷰,체험단"
            ));
        }
        return mocks;
    }
}
