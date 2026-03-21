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
public class StorynmediaCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(StorynmediaCrawler.class);
    private static final String BASE_URL = "https://storyn.co.kr";

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    private int campaignIndex = 0;

    public StorynmediaCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "STORYNMEDIA";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        String baseUrl = source.getBaseUrl();

        if (!robotsTxtChecker.isAllowed(baseUrl, "/")) {
            log.warn("STORYNMEDIA robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();
        campaignIndex = 0;

        try {
            Document doc = jsoupClient.fetch(baseUrl);

            // iMweb 기반 CMS - 위젯 기반 상품/캠페인 목록 탐색
            // .shop-content 영역의 상품 카드 또는 위젯 섹션 탐색
            Elements items = doc.select(".product-item, .shop-item, .widget-item, .prd-wrap a[href]");

            if (items.isEmpty()) {
                // Fallback: 이미지가 포함된 링크 요소 탐색
                items = doc.select("a[href]:has(img)");
            }

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("STORYNMEDIA 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("STORYNMEDIA 메인 페이지 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("STORYNMEDIA 크롤링 완료: {}건", results.size());
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

        Integer recruitCount = null;
        Matcher rm = Pattern.compile("모집\\s*(\\d+)").matcher(doc.text());
        if (rm.find()) recruitCount = Integer.parseInt(rm.group(1));

        String reward = null;
        for (Element el : doc.select("th, dt, .label, .prd-info dt")) {
            if (el.text().contains("제공") || el.text().contains("혜택")) {
                Element sibling = el.nextElementSibling();
                if (sibling != null) {
                    reward = sibling.text().trim();
                    break;
                }
            }
        }

        String mission = null;
        for (Element el : doc.select("th, dt, .label, .prd-info dt")) {
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
                coalesce(campaign.getRecruitCount(), recruitCount), campaign.getApplyStartDate(),
                coalesce(campaign.getApplyEndDate(), applyEndDate), campaign.getAnnouncementDate(),
                coalesce(campaign.getReward(), reward), coalesce(campaign.getMission(), mission),
                campaign.getAddress(), campaign.getKeywords(),
                coalesce(campaign.getCurrentApplicants(), currentApplicants)
        );
    }

    private CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String baseUrl = source.getBaseUrl();

        // 링크 URL 추출
        String href;
        if (item.tagName().equals("a")) {
            href = item.attr("href");
        } else {
            Element link = item.selectFirst("a[href]");
            if (link == null) return null;
            href = link.attr("href");
        }
        if (href.isEmpty() || href.equals("#")) return null;

        String originalUrl = href.startsWith("http") ? href : baseUrl + href;

        // 제목 추출
        String title = "";
        Element titleEl = item.selectFirst(".prd-name, .name, .title, h3, h4, strong");
        if (titleEl != null) {
            title = titleEl.text().trim();
        }
        if (title.isEmpty()) {
            title = item.attr("title");
        }
        if (title.isEmpty()) {
            Element img = item.selectFirst("img");
            if (img != null) title = img.attr("alt").trim();
        }
        if (title.isEmpty() || title.length() < 3) return null;

        // 썸네일 추출
        Element img = item.selectFirst("img");
        String thumbnailUrl = null;
        if (img != null) {
            thumbnailUrl = img.attr("src");
            if (thumbnailUrl.isEmpty()) thumbnailUrl = img.attr("data-src");
            if (thumbnailUrl != null && thumbnailUrl.startsWith("//")) {
                thumbnailUrl = "https:" + thumbnailUrl;
            } else if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
                thumbnailUrl = baseUrl + thumbnailUrl;
            }
        }

        campaignIndex++;
        String originalId = "storynmedia-" + campaignIndex;

        CampaignCategory category = CategoryMapper.map(title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                null, null, null, null,
                null, "블로그 리뷰 작성", null, "스토리앤미디어,체험단"
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
                    source.getCode(), "storynmedia-" + i,
                    "[스토리앤미디어] 체험단 캠페인 #" + i,
                    "스토리앤미디어 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=STORYNMEDIA+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",스토리앤미디어,체험단"
            ));
        }
        return mocks;
    }
}
