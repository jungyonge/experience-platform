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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AssaviewCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(AssaviewCrawler.class);
    private static final String BASE_URL = "https://assaview.co.kr";
    private static final String LIST_URL = BASE_URL + "/campaign_list.php";
    private static final Pattern CPID_PATTERN = Pattern.compile("cp_id=(\\d+)");
    private static final Pattern DAYS_LEFT_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("(\\d+)명");
    private static final DateTimeFormatter COUNTDOWN_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public AssaviewCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "ASSAVIEW";
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
            log.warn("ASSAVIEW robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(LIST_URL);
            Elements items = doc.select("#campaign_list_wrap > li[data-cp-id]");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("ASSAVIEW 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("ASSAVIEW 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("ASSAVIEW 크롤링 완료: {}건", results.size());
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
        Element missionEl = doc.selectFirst(".details .mission, .mission_info");
        if (missionEl != null) mission = missionEl.text().trim();

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

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String originalId = item.attr("data-cp-id");
        if (originalId.isEmpty()) return null;

        Element link = item.selectFirst("a[href*=campaign.php]");
        String originalUrl = BASE_URL + "/campaign.php?cp_id=" + originalId;
        if (link != null) {
            String href = link.attr("href");
            originalUrl = href.startsWith("http") ? href : BASE_URL + "/" + href;
        }

        Element titleEl = item.selectFirst(".details .subject");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        Element img = item.selectFirst(".imgBox > img");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + "/" + src.replaceFirst("^\\./", "");
            }
        }

        LocalDate applyEndDate = null;
        Element timer = item.selectFirst(".timer[data-countdown1]");
        if (timer != null) {
            try {
                String countdown = timer.attr("data-countdown1");
                LocalDateTime endDateTime = LocalDateTime.parse(countdown, COUNTDOWN_FMT);
                applyEndDate = endDateTime.toLocalDate();
            } catch (Exception ignored) {}
        }

        Element descEl = item.selectFirst(".desc .opt_name");
        String reward = descEl != null ? descEl.text().trim() : null;

        Integer recruitCount = null;
        Elements descSpans = item.select(".desc div span");
        for (Element span : descSpans) {
            String text = span.text();
            if (text.contains("명")) {
                Matcher m = RECRUIT_PATTERN.matcher(text);
                if (m.find()) {
                    recruitCount = Integer.parseInt(m.group(1));
                }
            }
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, "아싸뷰,체험단"
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.ETC};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 8 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "assaview-" + i,
                    "[아싸뷰] 체험단 캠페인 #" + i,
                    "아싸뷰 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=ASSAVIEW+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",아싸뷰,체험단"
            ));
        }
        return mocks;
    }
}
