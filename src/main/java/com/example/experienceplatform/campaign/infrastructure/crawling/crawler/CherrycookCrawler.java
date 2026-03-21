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
public class CherrycookCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(CherrycookCrawler.class);
    private static final String BASE_URL = "https://cherry-cook.com";
    private static final Pattern ID_PATTERN = Pattern.compile("/item/(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("(\\d+)일\\s*남음");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public CherrycookCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "CHERRYCOOK";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/mission")) {
            log.warn("CHERRYCOOK robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        try {
            String url = BASE_URL + "/mission";
            Document doc = jsoupClient.fetch(url);
            Elements items = doc.select("a[href^=/item/]");
            if (items.isEmpty()) {
                log.warn("CHERRYCOOK 캠페인 아이템을 찾지 못했습니다.");
                return results;
            }

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("CHERRYCOOK 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("CHERRYCOOK 크롤링 실패: {}", e.getMessage());
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("CHERRYCOOK 크롤링 완료: {}건", results.size());
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
        for (Element el : doc.select(".item-info dt, .info-label, th")) {
            if (el.text().contains("제공") || el.text().contains("리워드")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    reward = sibling.text().trim();
                    break;
                }
            }
        }

        String mission = null;
        for (Element el : doc.select(".item-info dt, .info-label, th")) {
            if (el.text().contains("미션") || el.text().contains("활동")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    mission = sibling.text().trim();
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
                coalesce(campaign.getReward(), reward), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // ID from href /item/{id}
        String href = item.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Title
        String title = item.select(".item-product").text().trim();
        if (title.isEmpty()) return null;

        // Thumbnail
        Element img = item.selectFirst(".image-box img");
        String thumbnailUrl = img != null ? img.attr("src") : null;
        if (thumbnailUrl != null && thumbnailUrl.isEmpty()) thumbnailUrl = null;

        // D-day -> applyEndDate
        LocalDate applyEndDate = null;
        String deadlineText = item.select(".item-deadline").text().trim();
        Matcher ddayMatcher = DDAY_PATTERN.matcher(deadlineText);
        if (ddayMatcher.find()) {
            int daysLeft = Integer.parseInt(ddayMatcher.group(1));
            applyEndDate = LocalDate.now().plusDays(daysLeft);
        }

        // Recruit count (total)
        Integer recruitCount = CrawlingNumberParser.parse(item.select(".item-visit").text());

        // Category from title
        CampaignCategory category = CategoryMapper.map(title);

        String originalUrl = BASE_URL + "/item/" + originalId;

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount, null, applyEndDate, null,
                null, null, null, "체리쿡,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.BEAUTY, CampaignCategory.FOOD, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "cherrycook-" + i,
                    "[체리쿡] 체험단 캠페인 #" + i,
                    "체리쿡 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=CHERRYCOOK+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",체리쿡,체험단"
            ));
        }
        return mocks;
    }
}
