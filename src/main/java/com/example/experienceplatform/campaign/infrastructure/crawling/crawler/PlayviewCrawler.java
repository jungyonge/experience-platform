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
public class PlayviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(PlayviewCrawler.class);
    private static final String BASE_URL = "https://playview.co.kr";
    private static final String LIST_URL = BASE_URL + "/campaign/list?type=l";
    private static final Pattern ID_PATTERN = Pattern.compile("cp_id=([^&]+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("신청\\s*(\\d+)\\s*/\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public PlayviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "PLAYVIEW";
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
            log.warn("PLAYVIEW robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(LIST_URL);
            Elements items = doc.select("div.campaign_list > div.item");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("PLAYVIEW 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("PLAYVIEW 페이지 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("PLAYVIEW 크롤링 완료: {}건", results.size());
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
        for (Element el : doc.select("th, dt, .tit, .label")) {
            if (el.text().contains("주소") || el.text().contains("위치")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    address = sibling.text().trim();
                    break;
                }
            }
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
                campaign.getApplyEndDate(), coalesce(campaign.getAnnouncementDate(), announcementDate),
                campaign.getReward(), campaign.getMission(),
                coalesce(campaign.getAddress(), address), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title
        Element titleEl = item.selectFirst("div.tit");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // ID from a.box href /campaign/view?cp_id={id}
        Element link = item.selectFirst("a.box, a[href*=campaign/view]");
        if (link == null) return null;
        String href = link.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Original URL
        String originalUrl = BASE_URL + "/campaign/view?cp_id=" + originalId;

        // Thumbnail
        String thumbnailUrl = null;
        Element img = item.selectFirst("div.img > img, div.img img");
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
            }
        }

        // D-day and status
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Element ddayEl = item.selectFirst("div.remain");
        if (ddayEl != null) {
            String ddayText = ddayEl.text().trim();
            if (ddayText.contains("종료")) {
                status = CampaignStatus.CLOSED;
            } else {
                Matcher ddayMatcher = DDAY_PATTERN.matcher(ddayText);
                if (ddayMatcher.find()) {
                    applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
                }
            }
        }

        // Recruit count from div.request "신청 N / M"
        Integer recruitCount = null;
        Element recruitEl = item.selectFirst("div.request");
        if (recruitEl != null) {
            Matcher recruitMatcher = RECRUIT_PATTERN.matcher(recruitEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(2));
            }
        }

        // Reward
        String reward = null;
        Element rewardEl = item.selectFirst("div.txt");
        if (rewardEl != null) {
            reward = rewardEl.text().trim();
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "플레이뷰,체험단"
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
                    source.getCode(), "playview-" + i,
                    "[플레이뷰] 체험단 캠페인 #" + i,
                    "플레이뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=PLAYVIEW+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",플레이뷰,체험단"
            ));
        }
        return mocks;
    }
}
