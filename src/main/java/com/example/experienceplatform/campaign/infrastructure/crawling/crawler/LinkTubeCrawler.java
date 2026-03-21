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
public class LinkTubeCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(LinkTubeCrawler.class);
    private static final String BASE_URL = "https://linktube.me";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("/product/(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public LinkTubeCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "LINKTUBE";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/product")) {
            log.warn("LINKTUBE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            // Express SSR 기반 - /product 페이지에서 ol > li > a 구조
            Document doc = jsoupClient.fetch(BASE_URL + "/product");

            // 캠페인 리스트: ol > li > a[href=/product/{id}]
            Elements items = doc.select("ol li a[href*=/product/]");

            if (items.isEmpty()) {
                // Fallback: 모든 product 링크
                items = doc.select("a[href*=/product/]");
            }

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("LINKTUBE 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("LINKTUBE 크롤링 실패: {}", e.getMessage());
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("LINKTUBE 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from og:description or .order-info
        String description = null;
        Element metaOgDesc = doc.selectFirst("meta[property=og:description]");
        if (metaOgDesc != null) description = metaOgDesc.attr("content");

        // reward from "협찬 정보" section or .tit-info
        String reward = null;
        for (Element el : doc.select(".tit-info, .order-info span, .order-info div")) {
            String text = el.text().trim();
            if (text.contains("상당") || text.contains("제공") || text.contains("원")) {
                reward = text;
                break;
            }
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                campaign.getDetailContent(),
                campaign.getThumbnailUrl(), campaign.getOriginalUrl(),
                campaign.getCategory(), campaign.getStatus(),
                campaign.getRecruitCount(), campaign.getApplyStartDate(),
                campaign.getApplyEndDate(), campaign.getAnnouncementDate(),
                coalesce(campaign.getReward(), reward), campaign.getMission(),
                campaign.getAddress(),
                campaign.getKeywords(),
                campaign.getCurrentApplicants()
        );
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String href = item.attr("href");
        Matcher idMatcher = PRODUCT_ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        // 제목 추출 - 카드 내부 텍스트 (위치 + SNS 아이콘 + 제목 + 별점 등이 혼합)
        // 이미지가 아닌 텍스트 노드에서 추출
        String fullText = item.text().trim();
        if (fullText.isEmpty() || fullText.length() < 3) return null;

        // 제목은 카드 텍스트에서 추출 - 위치, 별점, 하트 수 등을 제거
        String title = fullText
                .replaceAll("\\d+\\.\\d+", "")  // 별점 제거
                .replaceAll("광고비.*", "")  // 광고비 텍스트 제거
                .replaceAll("\\d+$", "")  // 마지막 숫자(하트수) 제거
                .trim();

        if (title.isEmpty() || title.length() < 3) {
            title = fullText;
        }
        // 제목이 너무 길면 잘라냄
        if (title.length() > 100) {
            title = title.substring(0, 100).trim();
        }

        // 썸네일 - img.thumb의 style background-image에서 추출 (src는 placeholder)
        String thumbnailUrl = null;
        Element img = item.selectFirst("img.thumb");
        if (img != null) {
            String style = img.attr("style");
            if (style != null && style.contains("background-image")) {
                java.util.regex.Matcher bgMatcher = java.util.regex.Pattern
                        .compile("url\\(['\"]?([^'\"\\)]+)['\"]?\\)").matcher(style);
                if (bgMatcher.find()) {
                    thumbnailUrl = bgMatcher.group(1);
                }
            }
            if (thumbnailUrl == null) {
                String src = img.attr("src");
                if (!src.isEmpty() && !src.contains("img_transparent")) {
                    thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
                }
            }
        }

        // SNS 타입 추출 (YouTube, Blog, Instagram 아이콘)
        List<String> snsTypes = new ArrayList<>();
        Elements snsIcons = item.select("img[src*=sns-]");
        for (Element icon : snsIcons) {
            String iconSrc = icon.attr("src");
            if (iconSrc.contains("youtube")) snsTypes.add("유튜브");
            if (iconSrc.contains("blog")) snsTypes.add("블로그");
            if (iconSrc.contains("insta")) snsTypes.add("인스타그램");
        }

        CampaignCategory category = CategoryMapper.map(title);
        String keywords = snsTypes.isEmpty() ? "링크튜브,체험단" :
                String.join(",", snsTypes) + ",링크튜브,체험단";

        String mission = snsTypes.contains("유튜브") ? "유튜브 리뷰 작성" :
                snsTypes.contains("인스타그램") ? "인스타그램 리뷰 작성" : "블로그 리뷰 작성";

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, CampaignStatus.RECRUITING,
                null, null, null, null,
                null, mission, null, keywords
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
                    source.getCode(), "linktube-" + i,
                    "[링크튜브] 체험단 캠페인 #" + i,
                    "링크튜브 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=LINKTUBE+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",링크튜브,체험단"
            ));
        }
        return mocks;
    }
}
