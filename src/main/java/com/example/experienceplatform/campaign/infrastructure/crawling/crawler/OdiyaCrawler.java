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
public class OdiyaCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(OdiyaCrawler.class);
    private static final String BASE_URL = "https://odiya.kr";
    private static final String LIST_URL = BASE_URL + "/category.php?category=829";
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");
    private static final Pattern ID_PATTERN = Pattern.compile("number=(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("모집\\s*(\\d+)");
    private static final Pattern APPLY_PATTERN = Pattern.compile("신청\\s*(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public OdiyaCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "ODIYA";
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
            log.warn("ODIYA robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(LIST_URL);
            Elements items = doc.select("div.over_row");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("ODIYA 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("ODIYA 페이지 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("ODIYA 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from meta
        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        // Odiya uses table-based layout similar to Ringble
        String fullText = doc.text();

        // reward from "제공내역" text pattern
        String reward = null;
        Matcher rewardMatcher = Pattern.compile("제공내역[:\\s]*([^\\n]{1,200})").matcher(fullText);
        if (rewardMatcher.find()) {
            reward = rewardMatcher.group(1).trim();
        }
        if (reward == null) {
            Element rewardHeader = doc.selectFirst("td:contains(제공), th:contains(제공)");
            if (rewardHeader != null) {
                Element rewardCell = rewardHeader.nextElementSibling();
                if (rewardCell != null && !rewardCell.text().isBlank()) {
                    reward = rewardCell.text().trim();
                }
            }
        }

        // address from table cell
        String address = null;
        Element addrHeader = doc.selectFirst("td:contains(주소), th:contains(주소)");
        if (addrHeader != null) {
            Element addrCell = addrHeader.nextElementSibling();
            if (addrCell != null && !addrCell.text().isBlank()) {
                address = addrCell.text().trim();
            }
        }

        // currentApplicants from "신청 X / 모집 Y"
        Integer currentApplicants = null;
        Matcher m = Pattern.compile("신청\\s*(\\d+)").matcher(fullText);
        if (m.find()) currentApplicants = Integer.parseInt(m.group(1));

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

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        // Title from td.font_17 strong
        Element titleEl = item.selectFirst("td.font_17 strong");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        // ID from table[onclick*=detail.php] onclick attribute
        String originalId = null;
        Element onclickEl = item.selectFirst("table[onclick*=detail.php]");
        if (onclickEl != null) {
            String onclick = onclickEl.attr("onclick");
            Matcher idMatcher = ID_PATTERN.matcher(onclick);
            if (idMatcher.find()) {
                originalId = idMatcher.group(1);
            }
        }
        if (originalId == null) return null;

        // Original URL
        String originalUrl = BASE_URL + "/detail.php?number=" + originalId + "&category=829";

        // Thumbnail from img[src*=mallimg]
        String thumbnailUrl = null;
        Element img = item.selectFirst("img[src*=mallimg]");
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

        // D-day from div.font_17 with D-N pattern
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Elements divs = item.select("div.font_17");
        for (Element div : divs) {
            String text = div.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(text);
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
                break;
            }
            if (text.contains("마감") || text.contains("종료")) {
                status = CampaignStatus.CLOSED;
            }
        }

        // Recruit count from strong text containing "모집"
        Integer recruitCount = null;
        Elements strongs = item.select("strong");
        for (Element strong : strongs) {
            String text = strong.text();
            if (text.contains("모집")) {
                Matcher recruitMatcher = RECRUIT_PATTERN.matcher(text);
                if (recruitMatcher.find()) {
                    recruitCount = Integer.parseInt(recruitMatcher.group(1));
                }
            }
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, "어디야,체험단"
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
                    source.getCode(), "odiya-" + i,
                    "[어디야] 체험단 캠페인 #" + i, "어디야 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=ODIYA+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",어디야,체험단"
            ));
        }
        return mocks;
    }
}
