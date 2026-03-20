package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
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
public class CometoplayCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(CometoplayCrawler.class);
    private static final String BASE_URL = "https://cometoplay.kr";
    private static final Pattern ITID_PATTERN = Pattern.compile("it_id=(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-day\\s*(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*<b[^>]*>(\\d+)</b>|모집\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public CometoplayCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "COMETOPLAY";
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
            log.warn("COMETOPLAY robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(BASE_URL + "/item_list.php");
            Elements items = doc.select("div.item_box_list li, div.item_box_list div:has(span.it_name)");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("COMETOPLAY 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("COMETOPLAY 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("COMETOPLAY 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from .itdes or meta
        String description = null;
        Element descEl = doc.selectFirst(".itdes");
        if (descEl != null) description = descEl.text().trim();
        if (description == null || description.isBlank()) {
            Element metaDesc = doc.selectFirst("meta[name=description]");
            if (metaDesc != null) description = metaDesc.attr("content");
        }

        // reward from .etc_list2 containing "제공내역"
        String reward = null;
        for (Element el : doc.select(".etc_list2")) {
            Element titEl = el.selectFirst(".tit_etc2");
            if (titEl != null && titEl.text().contains("제공내역")) {
                Element valEl = el.selectFirst(".etc2");
                if (valEl != null) reward = valEl.text().trim();
                break;
            }
        }

        // address from .campain_info containing "업체주소"
        String address = null;
        for (Element li : doc.select("li.campain_info")) {
            Element titEl = li.selectFirst("span.tit");
            if (titEl != null && titEl.text().contains("주소")) {
                Element infoEl = li.selectFirst("span.info");
                if (infoEl != null) {
                    address = infoEl.text().trim();
                    break;
                }
            }
        }

        // currentApplicants from "신청인원 X / 모집인원 Y"
        Integer currentApplicants = null;
        Matcher m = Pattern.compile("신청인원\\s*(\\d+)").matcher(doc.text());
        if (m.find()) currentApplicants = Integer.parseInt(m.group(1));

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                coalesce(campaign.getAddress(), address),
                campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        Element link = item.selectFirst("a[href*=item.php]");
        if (link == null) return null;

        String href = link.attr("href");
        Matcher idMatcher = ITID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        String originalUrl = href.startsWith("http") ? href : BASE_URL + "/" + href;

        Element titleEl = item.selectFirst("span.it_name");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        Element img = item.selectFirst("img.it_img");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + "/" + src.replaceFirst("^\\./", "");
            }
        }

        LocalDate applyEndDate = null;
        Element ddayEl = item.selectFirst("span.txt_num");
        if (ddayEl != null) {
            String ddayText = ddayEl.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(ddayText);
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
            }
        }

        Integer recruitCount = null;
        Element peoCntEl = item.selectFirst("span.peo_cnt");
        if (peoCntEl != null) {
            String peoCntHtml = peoCntEl.html();
            Pattern recruitPat = Pattern.compile("모집.*?(\\d+)");
            Matcher recruitMatcher = recruitPat.matcher(peoCntEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        Element descEl = item.selectFirst("span.it_description");
        String keywords = descEl != null ? descEl.text().replace("#", "").trim().replace(" ", ",") : null;

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null,
                keywords != null ? keywords + ",놀러와체험단" : "놀러와체험단,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.TRAVEL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 10; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "cometoplay-" + i,
                    "[놀러와체험단] 체험단 캠페인 #" + i,
                    "놀러와체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=COMETOPLAY+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",놀러와체험단,체험단"
            ));
        }
        return mocks;
    }
}
