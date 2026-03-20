package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;
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
public class RealReviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(RealReviewCrawler.class);
    private static final String BASE_URL = "https://www.real-review.kr";
    private static final Pattern RECRUIT_NUMBER_PATTERN = Pattern.compile("모집\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public RealReviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "REAL_REVIEW";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/explore")) {
            log.warn("REAL_REVIEW robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = BASE_URL + "/explore/" + (page > 1 ? "?&page=" + page : "");
                Document doc = jsoupClient.fetch(url);
                Elements items = doc.select("div[data-project-id]");
                if (items.isEmpty()) break;

                for (Element item : items) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("REAL_REVIEW 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("REAL_REVIEW 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("REAL_REVIEW 크롤링 완료: {}건", results.size());
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
        for (Element el : doc.select("th, dt, .label, ._o-label")) {
            if (el.text().contains("제공") || el.text().contains("리워드")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    reward = sibling.text().trim();
                    break;
                }
            }
        }

        String mission = null;
        for (Element el : doc.select("th, dt, .label, ._o-label")) {
            if (el.text().contains("미션") || el.text().contains("활동")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    mission = sibling.text().trim();
                    break;
                }
            }
        }

        LocalDate applyEndDate = null;
        Matcher dm = Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})").matcher(doc.text());
        if (dm.find()) {
            try {
                applyEndDate = LocalDate.of(
                    Integer.parseInt(dm.group(1)),
                    Integer.parseInt(dm.group(2)),
                    Integer.parseInt(dm.group(3)));
            } catch (Exception ignored) {}
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                coalesce(campaign.getApplyEndDate(), applyEndDate), campaign.getAnnouncementDate(),
                coalesce(campaign.getReward(), reward), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // ID from data-project-id attribute
        String originalId = item.attr("data-project-id").trim();
        if (originalId.isEmpty()) return null;

        // Title from a._o-title
        Element titleEl = item.selectFirst("a._o-title");
        String title = titleEl != null ? titleEl.text().trim() : "";
        if (title.isEmpty()) return null;

        // Original URL from a._o-title href
        String originalUrl = titleEl.attr("abs:href");
        if (originalUrl.isEmpty()) {
            originalUrl = titleEl.attr("href");
            if (!originalUrl.startsWith("http")) {
                originalUrl = BASE_URL + originalUrl;
            }
        }

        // Thumbnail from ._o-featured-image img
        Element img = item.selectFirst("._o-featured-image img");
        String thumbnailUrl = null;
        if (img != null) {
            thumbnailUrl = img.attr("src");
            if (thumbnailUrl.isEmpty()) {
                thumbnailUrl = img.attr("data-src");
            }
        }

        // Type labels
        CampaignCategory category = CampaignCategory.ETC;
        if (item.selectFirst("span._o-label._o-label--visit") != null) {
            category = CampaignCategory.FOOD; // 방문 -> likely food/local
        } else if (item.selectFirst("span._o-label._o-label--shipping") != null) {
            category = CampaignCategory.LIFE; // 배송 -> product/life
        } else if (item.selectFirst("span._o-label._o-label--payback") != null) {
            category = CampaignCategory.ETC; // 페이백
        }
        // Refine category from title
        category = CategoryMapper.map(title);

        // Recruit count from "모집 N"
        Integer recruitCount = null;
        Element recruitEl = item.selectFirst("span._o-label._o-label--recruitment");
        if (recruitEl != null) {
            Matcher recruitMatcher = RECRUIT_NUMBER_PATTERN.matcher(recruitEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        // Status from data-status attribute on span._o-recruitment-status
        CampaignStatus status = CampaignStatus.RECRUITING;
        Element statusEl = item.selectFirst("span._o-recruitment-status[data-status]");
        if (statusEl != null) {
            String dataStatus = statusEl.attr("data-status");
            if ("close".equalsIgnoreCase(dataStatus)) {
                status = CampaignStatus.CLOSED;
            }
        }

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl != null && !thumbnailUrl.isEmpty() ? thumbnailUrl : null,
                originalUrl, category, status,
                recruitCount, null, null, null,
                null, null, null, "리얼리뷰,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "real-review-" + i,
                    "[리얼리뷰] 체험단 캠페인 #" + i,
                    "리얼리뷰 체험단 설명 " + i,
                    "리얼리뷰 상세 내용 " + i,
                    "https://placehold.co/300x200?text=REAL_REVIEW+" + i,
                    BASE_URL + "/explore/real-review-" + i,
                    cat, status,
                    5 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",리얼리뷰,체험단"
            ));
        }
        return mocks;
    }
}
