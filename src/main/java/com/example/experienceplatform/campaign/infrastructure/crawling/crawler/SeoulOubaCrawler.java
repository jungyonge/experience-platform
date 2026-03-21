package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;
import org.jsoup.Jsoup;
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
import java.util.stream.Collectors;

@Component
public class SeoulOubaCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(SeoulOubaCrawler.class);
    private static final String MAIN_URL = "https://seoulouba.co.kr/campaign/";
    private static final String AJAX_URL = "https://seoulouba.co.kr/campaign/ajax/list.ajax.php";
    private static final Pattern ID_PATTERN = Pattern.compile("[?&]c=(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public SeoulOubaCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "SEOULOUBA";
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

        if (!robotsTxtChecker.isAllowed(baseUrl, "/campaign/")) {
            log.warn("SEOULOUBA robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        // Page 1: fetch main HTML page
        try {
            Document doc = jsoupClient.fetch(MAIN_URL);
            List<CrawledCampaign> page1 = parseCampaignItems(doc, source);
            results.addAll(page1);
            if (page1.isEmpty()) {
                return results;
            }
        } catch (Exception e) {
            log.error("SEOULOUBA 첫 페이지 크롤링 실패: {}", e.getMessage());
            return results;
        }

        // Pages 2+: fetch via AJAX POST
        for (int page = 2; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                delayHandler.delay();
                Document doc = fetchAjaxPage(page);
                List<CrawledCampaign> pageCampaigns = parseCampaignItems(doc, source);
                if (pageCampaigns.isEmpty()) break;
                results.addAll(pageCampaigns);
            } catch (Exception e) {
                log.error("SEOULOUBA 페이지 {} 크롤링 실패: {}", page, e.getMessage());
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
        for (Element el : doc.select("th, dt, .tit, .label, .s_campaign_addr")) {
            String text = el.text();
            if (text.contains("주소") || text.contains("위치")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    address = sibling.text().trim();
                    break;
                }
            }
        }
        if (address == null) {
            Element addrEl = doc.selectFirst(".s_campaign_addr, .campaign_addr, .address");
            if (addrEl != null) address = addrEl.text().trim();
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                campaign.getReward(), campaign.getMission(),
                coalesce(campaign.getAddress(), address), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private Document fetchAjaxPage(int page) {
        try {
            return Jsoup.connect(AJAX_URL)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getConnectionTimeoutMs())
                    .referrer(MAIN_URL)
                    .data("cat", "")
                    .data("qq", "")
                    .data("q", "")
                    .data("q1", "")
                    .data("q2", "")
                    .data("ar1", "")
                    .data("ar2", "")
                    .data("sort", "")
                    .data("page", String.valueOf(page))
                    .post();
        } catch (Exception e) {
            throw new CrawlingException("SEOULOUBA AJAX 페이지 조회 실패: page=" + page, e);
        }
    }

    private List<CrawledCampaign> parseCampaignItems(Document doc, CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();
        Elements items = doc.select("li.campaign_content");

        for (Element item : items) {
            try {
                CrawledCampaign campaign = parseItem(item, source);
                if (campaign != null) {
                    results.add(campaign);
                }
            } catch (Exception e) {
                log.warn("SEOULOUBA 아이템 파싱 실패: {}", e.getMessage());
            }
        }
        return results;
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Extract original ID and URL from href
        Element link = item.selectFirst("a[href*=?c=]");
        if (link == null) return null;

        String href = link.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Make absolute URL
        String originalUrl = href;
        if (!href.startsWith("http")) {
            originalUrl = "https://seoulouba.co.kr" + href;
        }

        // Title from .s_campaign_title
        Element titleEl = item.selectFirst(".s_campaign_title");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // Thumbnail from .tum_img img
        String thumbnailUrl = null;
        Element thumbImg = item.selectFirst(".tum_img img[src]");
        if (thumbImg != null) {
            String src = thumbImg.attr("src");
            if (src.startsWith("//")) {
                thumbnailUrl = "https:" + src;
            } else if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : "https://seoulouba.co.kr" + src;
            }
        }

        // Tags from .icon_tag span
        Elements tagElements = item.select(".icon_tag span");
        List<String> tags = tagElements.stream()
                .map(el -> el.text().trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
        String tagText = String.join(",", tags);

        // Reward from .basic_blue
        String reward = null;
        Element rewardEl = item.selectFirst(".basic_blue");
        if (rewardEl != null) {
            reward = rewardEl.text().trim();
        }

        // D-day from .d_day span → applyEndDate
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Element ddayEl = item.selectFirst(".d_day span");
        if (ddayEl != null) {
            String ddayText = ddayEl.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(ddayText);
            if (ddayMatcher.find()) {
                int daysLeft = Integer.parseInt(ddayMatcher.group(1));
                applyEndDate = LocalDate.now().plusDays(daysLeft);
            }
            if (ddayText.contains("마감") || ddayText.contains("D-0")) {
                status = CampaignStatus.CLOSED;
            }
        }

        // Recruit count from .recruit
        Integer recruitCount = null;
        Element recruitEl = item.selectFirst(".recruit");
        if (recruitEl != null) {
            Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        // Mission based on tags
        String mission = tags.contains("방문형")
                ? "방문 체험 후 블로그 리뷰" : "블로그 리뷰";

        // Category from tags and title
        String categoryInput = tagText + " " + title;
        CampaignCategory category = CategoryMapper.map(categoryInput);

        // Keywords
        String keywords = tagText.isEmpty() ? null : tagText;

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl,
                originalUrl,
                category, status,
                recruitCount,
                null, applyEndDate, null,
                reward, mission, null, keywords
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.FOOD, CampaignCategory.BEAUTY,
                CampaignCategory.TRAVEL, CampaignCategory.LIFE};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "seoulouba-" + i,
                    "[서울오빠] 체험단 캠페인 #" + i,
                    "서울오빠 체험단 설명 " + i,
                    "서울오빠 상세 내용 " + i,
                    "https://placehold.co/300x200?text=SEOULOUBA+" + i,
                    "https://seoulouba.co.kr/campaign/?c=" + (399700 + i),
                    cat, status,
                    3 + i % 8,
                    today.minusDays(5),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    i % 2 == 0 ? "방문 체험 후 블로그 리뷰" : "블로그 리뷰",
                    i % 3 == 0 ? "서울 강서구 마곡동 " + i : null,
                    "서울오빠,체험단," + cat.getDisplayName()
            ));
        }
        return mocks;
    }
}
