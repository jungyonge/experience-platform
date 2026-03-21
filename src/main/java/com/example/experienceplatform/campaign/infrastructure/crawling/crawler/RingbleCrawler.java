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
public class RingbleCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(RingbleCrawler.class);
    private static final String BASE_URL = "https://ringble.co.kr";
    private static final Pattern DDAY_PATTERN = Pattern.compile("(\\d+)일\\s*남음");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("number=(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public RingbleCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "RINGBLE";
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
            log.warn("RINGBLE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        // Fetch main page and deadline page
        String[] urls = {
                BASE_URL + "/",
                BASE_URL + "/html_file.php?file=category_deadline.html"
        };

        for (String url : urls) {
            try {
                Document doc = jsoupClient.fetch(url);
                List<CrawledCampaign> campaigns = parsePage(doc, source);
                results.addAll(campaigns);
                delayHandler.delay();
            } catch (Exception e) {
                log.error("RINGBLE 페이지 크롤링 실패 ({}): {}", url, e.getMessage());
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("RINGBLE 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from meta
        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) description = metaDesc.attr("content");

        // reward from text block containing "제공내역"
        String reward = null;
        String fullText = doc.text();
        Matcher rewardMatcher = Pattern.compile("제공내역[:\\s]*([^\\n]{1,200})").matcher(fullText);
        if (rewardMatcher.find()) {
            reward = rewardMatcher.group(1).trim();
        }

        // currentApplicants from "신청 X / 모집 Y"
        Integer currentApplicants = null;
        Matcher m = Pattern.compile("신청\\s*(\\d+)").matcher(fullText);
        if (m.find()) currentApplicants = Integer.parseInt(m.group(1));

        // announcementDate from "당첨자 발표일" text
        LocalDate announcementDate = null;
        Matcher dateMatcher = Pattern.compile("당첨자\\s*발표일[^\\d]*(\\d{2})년\\s*(\\d{2})월\\s*(\\d{2})일").matcher(fullText);
        if (dateMatcher.find()) {
            try {
                int year = 2000 + Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int day = Integer.parseInt(dateMatcher.group(3));
                announcementDate = LocalDate.of(year, month, day);
            } catch (Exception ignored) {}
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(),
                coalesce(campaign.getAnnouncementDate(), announcementDate),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                campaign.getAddress(),
                campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private List<CrawledCampaign> parsePage(Document doc, CrawlingSource source) {
        List<CrawledCampaign> results = new ArrayList<>();

        Elements cards = doc.select("td.store_list_wrap");
        for (Element card : cards) {
            try {
                CrawledCampaign campaign = parseCard(card, source);
                if (campaign != null) results.add(campaign);
            } catch (Exception e) {
                log.warn("RINGBLE 아이템 파싱 실패: {}", e.getMessage());
            }
        }

        return results;
    }

    private CrawledCampaign parseCard(Element card, CrawlingSource source) {
        // Extract ID from detail.php?number= link
        Element detailLink = card.selectFirst("a[href*=detail.php?number=]");
        if (detailLink == null) return null;

        String href = detailLink.attr("href");
        Matcher idMatcher = NUMBER_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // Title: second .list_title (font-size:14px) or any with longer text
        String title = null;
        Elements titleEls = card.select("a.list_title");
        for (Element el : titleEls) {
            String text = el.text().trim();
            if (text.length() > 3 && !text.contains("남음") && !text.contains("마감")) {
                title = text;
                break;
            }
        }
        if (title == null || title.isEmpty()) return null;

        String originalUrl = href.startsWith("http") ? href : BASE_URL + "/" + href;

        // Thumbnail
        Element img = card.selectFirst("a[href*=detail.php] > img");
        String thumbnailUrl = null;
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                thumbnailUrl = src.startsWith("http") ? src :
                        (src.startsWith("./") ? BASE_URL + "/" + src.substring(2) : BASE_URL + "/" + src);
            }
        }

        // D-day
        LocalDate applyEndDate = null;
        String cardText = card.text();
        Matcher ddayMatcher = DDAY_PATTERN.matcher(cardText);
        if (ddayMatcher.find()) {
            applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
        }

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                null, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, "링블,체험단"
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
                    source.getCode(), "ringble-" + i,
                    "[링블] 체험단 캠페인 #" + i, "링블 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=RINGBLE+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status, 3 + i % 6, today.minusDays(3), today.plusDays(5 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",링블,체험단"
            ));
        }
        return mocks;
    }
}
