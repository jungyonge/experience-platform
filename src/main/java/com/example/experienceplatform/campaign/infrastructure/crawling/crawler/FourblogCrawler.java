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

@Component
public class FourblogCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(FourblogCrawler.class);
    private static final String BASE_URL = "https://4blog.net";
    private static final Pattern CID_PATTERN = Pattern.compile("/campaign/(\\d+)/?");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public FourblogCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                            RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "FOURBLOG";
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
            log.warn("FOURBLOG robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            // 4blog.net 메인 페이지에서 캠페인 카드를 파싱
            // 카드 구조: a.nounderline > div.mainbox > div.main-img-div + div.camp-description
            // AJAX 엔드포인트 /loadMoreDataCategory도 있지만 POST 파라미터가 필요
            Document doc = jsoupClient.fetch(BASE_URL);

            // 캠페인 카드 링크 - /campaign/{CID}/ 패턴
            Elements cards = doc.select("a.nounderline[href*=/campaign/]");

            if (cards.isEmpty()) {
                // Fallback: 모든 캠페인 링크 탐색
                cards = doc.select("a[href*=/campaign/]");
            }

            for (Element card : cards) {
                try {
                    CrawledCampaign campaign = parseCard(card, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("FOURBLOG 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("FOURBLOG 크롤링 실패: {}", e.getMessage());
        }

        log.info("FOURBLOG 크롤링 완료: {}건", results.size());
        return results;
    }

    public CrawledCampaign parseCard(Element card, CrawlingSource source) {
        String href = card.attr("href");
        Matcher idMatcher = CID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        // 캠페인명 추출 - .camp-name 또는 camp-description 내부 텍스트
        String title = "";
        Element nameEl = card.selectFirst(".camp-name");
        if (nameEl != null) {
            title = nameEl.text().trim();
        }
        if (title.isEmpty()) {
            Element descEl = card.selectFirst(".camp-description");
            if (descEl != null) title = descEl.text().trim();
        }
        if (title.isEmpty()) title = card.text().trim();
        if (title.isEmpty() || title.length() < 3) return null;

        // 썸네일 추출 - CloudFront CDN
        String thumbnailUrl = null;
        Element img = card.selectFirst(".main-img-div img");
        if (img == null) img = card.selectFirst("img");
        if (img != null) {
            String src = img.attr("src");
            if (!src.isEmpty() && !src.contains("img_transparent")) {
                thumbnailUrl = src.startsWith("http") ? src : BASE_URL + src;
            }
            // data-src 또는 lazy-load 속성 확인
            if (thumbnailUrl == null) {
                String dataSrc = img.attr("data-src");
                if (!dataSrc.isEmpty()) {
                    thumbnailUrl = dataSrc.startsWith("http") ? dataSrc : BASE_URL + dataSrc;
                }
            }
        }

        // D-day 추출 - .remainDate
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        Element remainEl = card.selectFirst(".remainDate");
        if (remainEl != null) {
            String remainText = remainEl.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(remainText);
            if (ddayMatcher.find()) {
                int daysLeft = Integer.parseInt(ddayMatcher.group(1));
                applyEndDate = LocalDate.now().plusDays(daysLeft);
            }
            if (remainText.contains("오늘마감") || remainText.contains("D-0")) {
                applyEndDate = LocalDate.now();
            }
            if (remainText.contains("D+")) {
                status = CampaignStatus.CLOSED;
            }
        }

        // 카테고리 배지 - .label
        String categoryText = "";
        Elements labels = card.select(".label");
        List<String> tags = new ArrayList<>();
        for (Element label : labels) {
            String labelText = label.text().trim();
            if (!labelText.isEmpty()) {
                tags.add(labelText);
                categoryText += " " + labelText;
            }
        }

        CampaignCategory category = CategoryMapper.map(categoryText + " " + title);
        String keywords = tags.isEmpty() ? "포블로그,체험단" : String.join(",", tags) + ",포블로그,체험단";

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                null, null, applyEndDate, null,
                null, "블로그 리뷰 작성", null, keywords
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
                    source.getCode(), "fourblog-" + i,
                    "[포블로그] 체험단 캠페인 #" + i,
                    "포블로그 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=FOURBLOG+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",포블로그,체험단"
            ));
        }
        return mocks;
    }
}
