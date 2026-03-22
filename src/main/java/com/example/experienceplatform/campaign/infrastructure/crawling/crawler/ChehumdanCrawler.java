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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChehumdanCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ChehumdanCrawler.class);
    private static final String BASE_URL = "https://chehumdan.com";
    private static final int PAGE_SIZE = 40;
    private static final Pattern NUMBER_PARAM_PATTERN = Pattern.compile("number=(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");
    private static final Pattern APPLICANT_PATTERN = Pattern.compile("신청\\s*(\\d+)");
    private static final Pattern CATEGORY_ID_PATTERN = Pattern.compile("category\\.php\\?category=(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public ChehumdanCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "CHEHUMDAN";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/category.php")) {
            log.warn("CHEHUMDAN robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        try {
            Set<String> categoryIds = discoverCategories();
            if (categoryIds.isEmpty()) {
                log.warn("CHEHUMDAN 카테고리를 찾지 못했습니다. 전체 페이지로 폴백합니다.");
                results.addAll(crawlListPage(BASE_URL + "/html_file.php?file=all_campaign.html", source));
            } else {
                for (String categoryId : categoryIds) {
                    results.addAll(crawlCategory(categoryId, source));
                    delayHandler.delay();
                }
            }
        } catch (Exception e) {
            log.error("CHEHUMDAN 크롤링 실패: {}", e.getMessage());
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("CHEHUMDAN 크롤링 완료: {}건 (카테고리 {}개)", results.size(), discoverCategoriesSafe().size());
        return results;
    }

    /**
     * 메인 페이지에서 카테고리 링크를 자동으로 발견한다.
     * category.php?category={id} 패턴의 링크를 추출한다.
     */
    private Set<String> discoverCategories() {
        Set<String> categoryIds = new LinkedHashSet<>();
        try {
            Document mainPage = jsoupClient.fetch(BASE_URL);
            Elements links = mainPage.select("a[href*=category.php?category=]");
            for (Element link : links) {
                Matcher m = CATEGORY_ID_PATTERN.matcher(link.attr("href"));
                if (m.find()) {
                    categoryIds.add(m.group(1));
                }
            }
            log.info("CHEHUMDAN 발견된 카테고리: {}", categoryIds);
        } catch (Exception e) {
            log.warn("CHEHUMDAN 카테고리 탐색 실패: {}", e.getMessage());
        }
        return categoryIds;
    }

    private Set<String> discoverCategoriesSafe() {
        try {
            return discoverCategories();
        } catch (Exception e) {
            return Set.of();
        }
    }

    /**
     * 특정 카테고리의 모든 페이지를 순회하며 크롤링한다.
     * start=0, 40, 80, ... 으로 페이징하며, maxPagesPerSite 설정을 존중한다.
     */
    private List<CrawledCampaign> crawlCategory(String categoryId, CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();
        int maxPages = properties.getMaxPagesPerSite();
        Set<String> seenIds = new LinkedHashSet<>();

        for (int page = 0; page < maxPages; page++) {
            int start = page * PAGE_SIZE;
            String url = BASE_URL + "/category.php?start=" + start + "&category=" + categoryId;
            log.debug("CHEHUMDAN 크롤링: {}", url);

            List<CrawledCampaign> pageResults = crawlListPage(url, source);
            if (pageResults.isEmpty()) {
                break;
            }

            boolean hasNew = false;
            for (CrawledCampaign campaign : pageResults) {
                if (seenIds.add(campaign.getOriginalId())) {
                    results.add(campaign);
                    hasNew = true;
                }
            }

            if (!hasNew) {
                break;
            }

            delayHandler.delay();
        }

        log.info("CHEHUMDAN 카테고리 {} 크롤링: {}건", categoryId, results.size());
        return results;
    }

    /**
     * 단일 목록 페이지에서 캠페인 아이템들을 파싱한다.
     * category.php와 html_file.php 모두 동일한 HTML 구조(div.thum-box)를 사용한다.
     */
    private List<CrawledCampaign> crawlListPage(String url, CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();
        try {
            Document doc = jsoupClient.fetch(url);
            Elements items = doc.select("div.thum-box");
            if (items.isEmpty()) {
                return results;
            }

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("CHEHUMDAN 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("CHEHUMDAN 페이지 크롤링 실패 {}: {}", url, e.getMessage());
        }
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc == null) metaDesc = doc.selectFirst("meta[property=og:description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        Integer currentApplicants = null;
        Matcher m = APPLICANT_PATTERN.matcher(doc.text());
        if (m.find()) currentApplicants = Integer.parseInt(m.group(1));

        String mission = null;
        for (Element el : doc.select("th, dt, .tit")) {
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

        String detailContent = DetailPageEnricher.extractDetailContent(doc);
        LocalDate announcementDate = DetailPageEnricher.extractAnnouncementDate(doc);
        LocalDate applyStartDate = DetailPageEnricher.extractApplyStartDate(doc);

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), coalesce(campaign.getApplyStartDate(), applyStartDate),
                coalesce(campaign.getApplyEndDate(), applyEndDate), coalesce(campaign.getAnnouncementDate(), announcementDate),
                campaign.getReward(), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // 제목: p.list-tit > a
        Element titleLink = item.selectFirst("p.list-tit > a");
        String title = titleLink != null ? titleLink.text().trim() : "";
        if (title.isEmpty()) return null;

        // ID: href에서 number 파라미터 추출 (detail.php?number={id}&category=...)
        String originalId = null;
        String linkHref = titleLink != null ? titleLink.attr("href") : "";
        if (linkHref.isEmpty()) {
            Element imgLink = item.selectFirst("div.thum-img > a");
            if (imgLink != null) linkHref = imgLink.attr("href");
        }
        Matcher idMatcher = NUMBER_PARAM_PATTERN.matcher(linkHref);
        if (idMatcher.find()) {
            originalId = idMatcher.group(1);
        }
        if (originalId == null || originalId.isEmpty()) return null;

        // 썸네일: div.thum-img img
        Element img = item.selectFirst("div.thum-img img");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                if (src.startsWith("./")) {
                    thumbnailUrl = BASE_URL + "/" + src.substring(2);
                } else if (src.startsWith("http")) {
                    thumbnailUrl = src;
                } else {
                    thumbnailUrl = BASE_URL + "/" + src;
                }
            }
        }

        // 모집인원: span.list-per (예: "신청 2 / 모집 10")
        String recruitText = item.select("span.list-per").text().trim();
        Integer recruitCount = null;
        Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitText);
        if (recruitMatcher.find()) {
            recruitCount = Integer.parseInt(recruitMatcher.group(1));
        }

        // 현재 신청자 수
        Integer currentApplicants = null;
        Matcher applicantMatcher = APPLICANT_PATTERN.matcher(recruitText);
        if (applicantMatcher.find()) {
            currentApplicants = Integer.parseInt(applicantMatcher.group(1));
        }

        // 상태: span.blink 텍스트
        CampaignStatus status = CampaignStatus.RECRUITING;
        String blinkText = item.select("span.blink").text().trim();
        if (blinkText.contains("마감")) {
            status = CampaignStatus.CLOSED;
        }

        // 제공내역: p.list-txt
        String reward = item.select("p.list-txt").text().trim();
        if (reward.isEmpty()) reward = null;

        // 지역: list-day 내 [지역] 패턴
        String address = null;
        String dayText = item.select("p.list-day").text();
        Matcher addrMatcher = Pattern.compile("\\[([^]]+)]").matcher(dayText);
        if (addrMatcher.find()) {
            address = addrMatcher.group(1).trim();
        }

        // 카테고리 추론
        CampaignCategory category = CategoryMapper.map(title);

        String originalUrl = BASE_URL + "/detail.php?number=" + originalId;

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, null, null,
                reward, null, address, "체험단닷컴,체험단",
                currentApplicants
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
                    source.getCode(), "chehumdan-" + i,
                    "[체험단닷컴] 체험단 캠페인 #" + i,
                    "체험단닷컴 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=CHEHUMDAN+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",체험단닷컴,체험단"
            ));
        }
        return mocks;
    }
}
