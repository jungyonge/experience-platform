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
public class DdokCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(DdokCrawler.class);
    private static final String BASE_URL = "https://ddok.co.kr";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public DdokCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "DDOK";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private static final Pattern ID_PATTERN = Pattern.compile("uidLink\\s*\\(\\s*'campaign'\\s*,\\s*(\\d+)");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("신청\\s*(\\d+)\\s*/\\s*(\\d+)");

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/")) {
            log.warn("DDOK robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        try {
            String url = BASE_URL + "/?m=campaign";
            Document doc = jsoupClient.fetch(url);
            Elements items = doc.select("div.cpitem");
            if (items.isEmpty()) {
                log.warn("DDOK 캠페인 아이템을 찾지 못했습니다.");
                return results;
            }

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("DDOK 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("DDOK 크롤링 실패: {}", e.getMessage());
        }
        results = enricher.enrich(results, this::parseDetailPage);
        log.info("DDOK 크롤링 완료: {}건", results.size());
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
        for (Element el : doc.select("th, dt, .tit")) {
            if (el.text().contains("제공") || el.text().contains("혜택")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    reward = sibling.text().trim();
                    break;
                }
            }
        }

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
                coalesce(campaign.getReward(), reward), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title
        String title = item.select("div.sbj").text().trim();
        if (title.isEmpty()) return null;

        // ID from div.photo[onclick] - parse uidLink('campaign',{id},'view',...)
        String originalId = null;
        Element photoDiv = item.selectFirst("div.photo[onclick]");
        if (photoDiv != null) {
            String onclick = photoDiv.attr("onclick");
            Matcher idMatcher = ID_PATTERN.matcher(onclick);
            if (idMatcher.find()) {
                originalId = idMatcher.group(1);
            }
        }
        if (originalId == null || originalId.isEmpty()) return null;

        // Thumbnail
        Element img = item.selectFirst("img.tm");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + "/" + src;
            }
        }

        // D-day -> applyEndDate
        LocalDate applyEndDate = null;
        String ddayText = item.select("div.num span.b").text().trim();
        Matcher ddayMatcher = DDAY_PATTERN.matcher(ddayText);
        if (ddayMatcher.find()) {
            int daysLeft = Integer.parseInt(ddayMatcher.group(1));
            applyEndDate = LocalDate.now().plusDays(daysLeft);
        }

        // Recruit count from "신청 N/M"
        Integer recruitCount = null;
        String scoreText = item.select("div.score").text().trim();
        Matcher recruitMatcher = RECRUIT_PATTERN.matcher(scoreText);
        if (recruitMatcher.find()) {
            recruitCount = Integer.parseInt(recruitMatcher.group(2));
        }

        // Type
        String typeText = item.select("div.cp_type").text().trim();
        CampaignCategory category = CategoryMapper.map(typeText + " " + title);

        String originalUrl = BASE_URL + "/?m=campaign&uid=" + originalId;

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount, null, applyEndDate, null,
                null, null, null, "똑똑체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "ddok-" + i,
                    "[똑똑체험단] 체험단 캠페인 #" + i,
                    "똑똑체험단 설명 " + i,
                    "똑똑체험단 상세 내용 " + i,
                    "https://placehold.co/300x200?text=DDOK+" + i,
                    BASE_URL + "/?m=campaign&vid=ddok-" + i,
                    cat, status,
                    5 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",똑똑체험단"
            ));
        }
        return mocks;
    }
}
