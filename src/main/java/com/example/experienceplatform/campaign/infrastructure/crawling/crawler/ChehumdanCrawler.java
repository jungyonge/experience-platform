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
public class ChehumdanCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ChehumdanCrawler.class);
    private static final String BASE_URL = "https://chehumdan.com";
    private static final Pattern NUMBER_PARAM_PATTERN = Pattern.compile("number=(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");

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
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/html_file.php")) {
            log.warn("CHEHUMDAN robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        try {
            String url = BASE_URL + "/html_file.php?file=all_campaign.html";
            Document doc = jsoupClient.fetch(url);
            Elements items = doc.select("div.thum-box");
            if (items.isEmpty()) {
                log.warn("CHEHUMDAN 캠페인 아이템을 찾지 못했습니다.");
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
            log.error("CHEHUMDAN 크롤링 실패: {}", e.getMessage());
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("CHEHUMDAN 크롤링 완료: {}건", results.size());
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

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                coalesce(campaign.getApplyEndDate(), applyEndDate), campaign.getAnnouncementDate(),
                campaign.getReward(), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title from p.list-tit > a
        Element titleLink = item.selectFirst("p.list-tit > a");
        String title = titleLink != null ? titleLink.text().trim() : "";
        if (title.isEmpty()) return null;

        // ID from href - extract number parameter from detail.php?number={id}&category=...
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

        // Thumbnail from div.thum-img img (relative ./mallimg/..., prefix with BASE_URL + "/")
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

        // Recruit from span.list-per (e.g., "신청 2 / 모집 10")
        String recruitText = item.select("span.list-per").text().trim();
        Integer recruitCount = null;
        Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitText);
        if (recruitMatcher.find()) {
            recruitCount = Integer.parseInt(recruitMatcher.group(1));
        }

        // Status from span.blink
        CampaignStatus status = CampaignStatus.RECRUITING;
        String blinkText = item.select("span.blink").text().trim();

        // Reward from p.list-txt
        String reward = item.select("p.list-txt").text().trim();
        if (reward.isEmpty()) reward = null;

        // Category from title
        CampaignCategory category = CategoryMapper.map(title);

        String originalUrl = BASE_URL + "/detail.php?number=" + originalId;

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, null, null,
                reward, null, null, "체험단닷컴,체험단"
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
