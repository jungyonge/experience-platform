package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
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

import static com.example.experienceplatform.campaign.infrastructure.crawling.DetailPageEnricher.coalesce;

@Component
public class GabojaCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(GabojaCrawler.class);
    private static final String BASE_URL = "https://xn--o39a04kpnjo4k9hgflp.com";
    private static final String AJAX_URL = BASE_URL + "/main/ajax/_ajax.cmpMainList.php";
    private static final Pattern ID_PATTERN = Pattern.compile("[?&]id=(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");
    private static final String[] SECTIONS = {"pick", "new", "closing", "hit"};

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public GabojaCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "GABOJA";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();

        for (String section : SECTIONS) {
            try {
                Document doc = Jsoup.connect(AJAX_URL)
                        .userAgent(properties.getUserAgent())
                        .timeout(properties.getConnectionTimeoutMs())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .referrer(BASE_URL)
                        .data("section", section)
                        .data("tag", "")
                        .post();

                Elements cards = doc.select("a.slick_link[href]");
                for (Element card : cards) {
                    try {
                        CrawledCampaign campaign = parseItem(card, source);
                        if (campaign != null) results.add(campaign);
                    } catch (Exception e) {
                        log.warn("GABOJA 아이템 파싱 실패: {}", e.getMessage());
                    }
                }
                delayHandler.delay();
            } catch (Exception e) {
                log.error("GABOJA {} 섹션 크롤링 실패: {}", section, e.getMessage());
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("GABOJA 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from meta
        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        String fullText = doc.text();

        // reward from "제공 내역" section
        String reward = null;
        Matcher rewardMatcher = Pattern.compile("제공\\s*내역[:\\s]*([^\\n]{1,300})").matcher(fullText);
        if (rewardMatcher.find()) {
            reward = rewardMatcher.group(1).trim();
        }

        // address from "방문 및 예약" section - look for address-like text
        String address = null;
        for (Element el : doc.select("td, th, dt, dd, span, div, p")) {
            String text = el.ownText().trim();
            if (text.matches(".*(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)(?:특별시|광역시|도)?\\s*[가-힣]+(?:시|군|구|동|읍|면).*") && text.length() < 150 && text.length() > 5) {
                address = text;
                break;
            }
        }

        // currentApplicants from "신청자 X/Y" pattern
        Integer currentApplicants = null;
        Matcher m = Pattern.compile("신청자\\s*(\\d+)").matcher(fullText);
        if (m.find()) currentApplicants = Integer.parseInt(m.group(1));
        if (currentApplicants == null) {
            Matcher m2 = Pattern.compile("신청\\s*(\\d+)").matcher(fullText);
            if (m2.find()) currentApplicants = Integer.parseInt(m2.group(1));
        }

        String detailContent = DetailPageEnricher.extractDetailContent(doc);
        LocalDate announcementDate = DetailPageEnricher.extractAnnouncementDate(doc);
        LocalDate applyStartDate = DetailPageEnricher.extractApplyStartDate(doc);

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), coalesce(campaign.getApplyStartDate(), applyStartDate),
                campaign.getApplyEndDate(), coalesce(campaign.getAnnouncementDate(), announcementDate),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                coalesce(campaign.getAddress(), address),
                campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element card, CrawlingSource source) {
        String href = card.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        String title = card.select(".info_area dl dt").text().trim();
        if (title.isEmpty()) return null;

        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        Element img = card.selectFirst(".img_area img");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
            }
        }

        String reward = card.select(".info_area dl dd").text().trim();
        if (reward.isEmpty()) reward = null;

        LocalDate applyEndDate = null;
        Elements cateItems = card.select(".cate li");
        for (Element li : cateItems) {
            Matcher ddayMatcher = DDAY_PATTERN.matcher(li.text());
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
            }
        }

        Integer recruitCount = null;
        String currentText = card.select(".current").text();
        Matcher recruitMatcher = RECRUIT_PATTERN.matcher(currentText);
        if (recruitMatcher.find()) {
            recruitCount = Integer.parseInt(recruitMatcher.group(1));
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "가보자체험단,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.TRAVEL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "gaboja-" + i,
                    "[가보자체험단] 체험단 캠페인 #" + i,
                    "가보자체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=GABOJA+" + i,
                    source.getBaseUrl() + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",가보자체험단,체험단"
            ));
        }
        return mocks;
    }
}
