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
public class HukiCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(HukiCrawler.class);
    private static final String BASE_URL = "https://www.huki.co.kr";
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern WR_ID_PATTERN = Pattern.compile("wr_id=(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("신청\\s*(\\d+)\\s*/\\s*(\\d+)");
    private static final String[] CAMPAIGN_PATHS = {
            "/theme/huki/skin/board/gallery/productCampaign.php",
            "/theme/huki/skin/board/gallery/localCampaign.php",
            "/theme/huki/skin/board/gallery/reviewCampaign.php",
            "/theme/huki/skin/board/gallery/newsCampaign.php"
    };

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public HukiCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "HUKI";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/theme/huki/skin/board/gallery/")) {
            log.warn("HUKI robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        for (String path : CAMPAIGN_PATHS) {
            try {
                String url = BASE_URL + path;
                Document doc = jsoupClient.fetch(url);
                Elements items = doc.select("ul.c_list > li");
                if (items.isEmpty()) {
                    log.debug("HUKI {} 에서 아이템을 찾지 못했습니다.", path);
                    continue;
                }

                for (Element item : items) {
                    try {
                        CrawledCampaign campaign = parseItem(item, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("HUKI 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
                delayHandler.delay();
            } catch (Exception e) {
                log.error("HUKI {} 크롤링 실패: {}", path, e.getMessage());
            }
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("HUKI 크롤링 완료: {}건", results.size());
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

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(), campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                campaign.getReward(), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title from p.t_tit
        String title = item.select("p.t_tit").text().trim();
        if (title.isEmpty()) return null;

        // ID from a[data-wr_id] or parse wr_id= from href
        String originalId = null;
        Element idLink = item.selectFirst("a[data-wr_id]");
        if (idLink != null) {
            originalId = idLink.attr("data-wr_id").trim();
        }
        if (originalId == null || originalId.isEmpty()) {
            Element anyLink = item.selectFirst("a[href]");
            if (anyLink != null) {
                Matcher wrIdMatcher = WR_ID_PATTERN.matcher(anyLink.attr("href"));
                if (wrIdMatcher.find()) {
                    originalId = wrIdMatcher.group(1);
                }
            }
        }
        if (originalId == null || originalId.isEmpty()) return null;

        // Thumbnail from div.img > img
        Element img = item.selectFirst("div.img > img");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
            }
        }

        // D-day from p.t_top -> applyEndDate
        LocalDate applyEndDate = null;
        String ddayText = item.select("p.t_top").text().trim();
        Matcher ddayMatcher = DDAY_PATTERN.matcher(ddayText);
        if (ddayMatcher.find()) {
            int daysLeft = Integer.parseInt(ddayMatcher.group(1));
            applyEndDate = LocalDate.now().plusDays(daysLeft);
        }

        // Recruit from p.t_bottom (e.g., "신청 4 / 5명")
        Integer recruitCount = null;
        String recruitText = item.select("p.t_bottom").text().trim();
        Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitText);
        if (recruitMatcher.find()) {
            recruitCount = Integer.parseInt(recruitMatcher.group(2));
        }

        // Reward from p.t_sub
        String reward = item.select("p.t_sub").text().trim();
        if (reward.isEmpty()) reward = null;

        // Category from title
        CampaignCategory category = CategoryMapper.map(title);

        String originalUrl = BASE_URL + "/bbs/board.php?bo_table=campaign&wr_id=" + originalId;

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount, null, applyEndDate, null,
                reward, null, null, "후키,체험단"
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
                    source.getCode(), "huki-" + i,
                    "[후키] 체험단 캠페인 #" + i,
                    "후키 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=HUKI+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",후키,체험단"
            ));
        }
        return mocks;
    }
}
