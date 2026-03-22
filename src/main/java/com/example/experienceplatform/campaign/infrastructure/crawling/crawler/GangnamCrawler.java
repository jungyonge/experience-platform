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
public class GangnamCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(GangnamCrawler.class);
    private static final Pattern ID_PATTERN = Pattern.compile("id=(\\d+)");
    private static final Pattern DAYS_LEFT_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public GangnamCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "GANGNAM";
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

        if (!robotsTxtChecker.isAllowed(baseUrl, "/theme/go/")) {
            log.warn("GANGNAM robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = buildPageUrl(source, page);
                Document doc = jsoupClient.fetch(url);
                Elements items = doc.select("li:has(a[href*=/cp/?id=])");
                if (items.isEmpty()) break;

                for (Element item : items) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("GANGNAM 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("GANGNAM 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }
        results = enricher.enrich(results, this::parseDetailPage);
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

        String address = null;
        for (Element li : doc.select("ul.basic_form li, .info_list li, .detail_info li")) {
            String text = li.text();
            if (text.contains("주소") || text.contains("위치")) {
                address = text.replaceFirst(".*?(주소|위치)\\s*:?\\s*", "").trim();
                if (!address.isEmpty()) break;
                address = null;
            }
        }

        String detailContent = DetailPageEnricher.extractDetailContent(doc);
        LocalDate announcementDate = DetailPageEnricher.extractAnnouncementDate(doc);
        LocalDate applyStartDate = DetailPageEnricher.extractApplyStartDate(doc);
        String keywords = "강남맛집,체험단";

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), coalesce(campaign.getApplyStartDate(), applyStartDate),
                campaign.getApplyEndDate(), coalesce(campaign.getAnnouncementDate(), announcementDate),
                campaign.getReward(), campaign.getMission(),
                coalesce(campaign.getAddress(), address), coalesce(campaign.getKeywords(), keywords),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private String buildPageUrl(CrawlingSource source, int page) {
        if (source.getListUrlPattern() != null && !source.getListUrlPattern().isBlank()) {
            return source.getListUrlPattern().replace("{page}", String.valueOf(page));
        }
        return source.getBaseUrl() + "/campaign?page=" + page;
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String baseUrl = source.getBaseUrl();

        // Extract originalId from href containing /cp/?id=
        Element idLink = item.selectFirst("a[href*=/cp/?id=]");
        if (idLink == null) return null;

        String href = idLink.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Title: <a> with /cp/?id= link that has text content
        String title = null;
        Elements cpLinks = item.select("a[href*=/cp/?id=]");
        for (Element link : cpLinks) {
            String text = link.ownText().trim();
            if (!text.isEmpty()) {
                title = text;
                break;
            }
        }
        if (title == null || title.isEmpty()) return null;

        // Thumbnail: img src with https: prefix for protocol-relative URLs
        String thumbnailUrl = null;
        Element img = item.selectFirst("img");
        if (img != null) {
            String src = img.attr("src");
            if (src.startsWith("//")) {
                thumbnailUrl = "https:" + src;
            } else if (!src.isEmpty()) {
                thumbnailUrl = src;
            }
        }

        // Original URL
        String originalUrl = baseUrl + href;

        // Badge/span text for status, mission, deadline
        String badgeText = "";
        Element badge = item.selectFirst("span");
        if (badge != null) {
            badgeText = badge.text();
        }

        // Status: "남음" in badge means RECRUITING
        CampaignStatus status = badgeText.contains("남음")
                ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;

        // Apply end date: "N일 남음" → LocalDate.now().plusDays(N)
        LocalDate applyEndDate = null;
        Matcher daysMatcher = DAYS_LEFT_PATTERN.matcher(badgeText);
        if (daysMatcher.find()) {
            int days = Integer.parseInt(daysMatcher.group(1));
            applyEndDate = LocalDate.now().plusDays(days);
        }

        // Mission: based on badge text
        String mission = badgeText.contains("방문형")
                ? "방문 체험 후 블로그 리뷰" : "블로그 리뷰";

        // Reward: first <div> text
        String reward = null;
        Element firstDiv = item.selectFirst("div");
        if (firstDiv != null) {
            String divText = firstDiv.text().trim();
            if (!divText.isEmpty()) {
                reward = divText;
            }
        }

        // Recruit count: from div containing "모집"
        Integer recruitCount = null;
        Elements divs = item.select("div");
        for (Element div : divs) {
            String divText = div.text();
            if (divText.contains("모집")) {
                Matcher recruitMatcher = RECRUIT_PATTERN.matcher(divText);
                if (recruitMatcher.find()) {
                    recruitCount = Integer.parseInt(recruitMatcher.group(1));
                }
                break;
            }
        }

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl,
                originalUrl,
                CampaignCategory.FOOD, status,
                recruitCount,
                null, applyEndDate, null,
                reward, mission, null, null
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.FOOD, CampaignCategory.BEAUTY,
                CampaignCategory.TRAVEL, CampaignCategory.LIFE};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "gangnam-" + i,
                    "[강남맛집] 체험단 캠페인 #" + i,
                    "강남맛집 체험단 설명 " + i,
                    "강남맛집 상세 내용 " + i,
                    "https://placehold.co/300x200?text=GANGNAM+" + i,
                    source.getBaseUrl() + "/campaign/gangnam-" + i,
                    cat, status,
                    2 + i % 6,
                    today.minusDays(7),
                    today.plusDays(5 + i),
                    today.plusDays(10 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰",
                    "서울 강남구 " + i + "번지",
                    "강남맛집," + cat.getDisplayName()
            ));
        }
        return mocks;
    }
}
