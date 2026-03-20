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
public class ReviewPlaceCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ReviewPlaceCrawler.class);
    private static final String BASE_URL = "https://reviewplace.co.kr";
    private static final Pattern ID_PATTERN = Pattern.compile("id=(\\d+)");
    private static final Pattern RECRUIT_PATTERN = Pattern.compile("/\\s*(\\d+)명");
    private static final Pattern DDAY_PATTERN = Pattern.compile("D-(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public ReviewPlaceCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                              RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "REVIEWPLACE";
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
            log.warn("REVIEWPLACE robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        try {
            Document doc = jsoupClient.fetch(BASE_URL);
            Elements items = doc.select("div.item a[href*=/pr/?id=]");

            for (Element item : items) {
                try {
                    CrawledCampaign campaign = parseItem(item, source);
                    if (campaign != null) results.add(campaign);
                } catch (Exception e) {
                    log.warn("REVIEWPLACE 아이템 파싱 실패: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("REVIEWPLACE 크롤링 실패: {}", e.getMessage());
        }

        log.info("REVIEWPLACE 크롤링 완료: {}건", results.size());
        return results;
    }

    public CrawledCampaign parseItem(Element item, CrawlingSource source) {
        String href = item.attr("href");
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        String originalUrl = href.startsWith("http") ? href : BASE_URL + href;

        Element titleEl = item.selectFirst(".tit");
        if (titleEl == null) return null;
        String title = titleEl.text().trim();
        if (title.isEmpty()) return null;

        Element descEl = item.selectFirst(".txt");
        String reward = descEl != null ? descEl.text().trim() : null;

        Element thumbEl = item.selectFirst("img.thumbimg");
        String thumbnailUrl = null;
        if (thumbEl != null) {
            thumbnailUrl = thumbEl.attr("src");
            if (thumbnailUrl.startsWith("//")) thumbnailUrl = "https:" + thumbnailUrl;
        }

        Element dateEl = item.selectFirst(".date");
        LocalDate applyEndDate = null;
        CampaignStatus status = CampaignStatus.RECRUITING;
        if (dateEl != null) {
            String dateText = dateEl.text().trim();
            Matcher ddayMatcher = DDAY_PATTERN.matcher(dateText);
            if (ddayMatcher.find()) {
                applyEndDate = LocalDate.now().plusDays(Integer.parseInt(ddayMatcher.group(1)));
            } else if (dateText.contains("오늘마감") || dateText.contains("오늘 마감")) {
                applyEndDate = LocalDate.now();
            }
            if (dateText.contains("마감")) {
                // 오늘마감은 아직 모집중
            }
        }

        Integer recruitCount = null;
        Element numEl = item.selectFirst(".num");
        if (numEl != null) {
            Matcher recruitMatcher = RECRUIT_PATTERN.matcher(numEl.text());
            if (recruitMatcher.find()) {
                recruitCount = Integer.parseInt(recruitMatcher.group(1));
            }
        }

        Elements tagEls = item.select(".txt_tag");
        List<String> tags = new ArrayList<>();
        for (Element tag : tagEls) {
            String t = tag.text().trim();
            if (!t.isEmpty()) tags.add(t);
        }

        CampaignCategory category = CategoryMapper.map(title + " " + String.join(" ", tags));
        String keywords = tags.isEmpty() ? "리뷰플레이스,체험단" : String.join(",", tags) + ",체험단";

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                thumbnailUrl, originalUrl, category, status,
                recruitCount, null, applyEndDate, null,
                reward, "블로그 리뷰 작성", null, keywords
        );
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.TRAVEL,
                CampaignCategory.LIFE, CampaignCategory.DIGITAL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 12; i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "reviewplace-" + i,
                    "[리뷰플레이스] 체험단 캠페인 #" + i, "리뷰플레이스 설명 " + i, null,
                    "https://placehold.co/300x200?text=REVIEWPLACE+" + i,
                    BASE_URL + "/pr/?id=" + (274600 + i),
                    cat, status, 5 + i % 10, today.minusDays(3), today.plusDays(7 + i), null,
                    "제공 내역 " + i, "블로그 리뷰 작성", null,
                    cat.getDisplayName() + ",리뷰플레이스,체험단"
            ));
        }
        return mocks;
    }
}
