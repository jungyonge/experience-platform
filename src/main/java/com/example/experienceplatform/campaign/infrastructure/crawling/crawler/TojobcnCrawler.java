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
public class TojobcnCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(TojobcnCrawler.class);
    private static final String BASE_URL = "https://www.tojobcn.com";
    private static final String BOARD_URL = BASE_URL + "/bbs/board.php?bo_table=gonggo_blog";
    private static final Pattern WR_ID_PATTERN = Pattern.compile("wr_id=(\\d+)");

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;
    private final DetailPageEnricher enricher;

    public TojobcnCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
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
        return "TOJOBCN";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        if (!robotsTxtChecker.isAllowed(BASE_URL, "/bbs/")) {
            log.warn("TOJOBCN robots.txt에 의해 크롤링이 차단되었습니다.");
            return List.of();
        }

        List<CrawledCampaign> results = new ArrayList<>();

        for (int page = 1; page <= properties.getMaxPagesPerSite(); page++) {
            try {
                String url = BOARD_URL + "&page=" + page;
                Document doc = jsoupClient.fetch(url);

                // Gnuboard 기반 게시판 - list-item 구조
                Elements rows = doc.select("li.list-item:not(.bg-light)");
                if (rows.isEmpty()) {
                    rows = doc.select("li.list-item");
                }

                boolean foundAny = false;
                for (Element row : rows) {
                    try {
                        CrawledCampaign campaign = parseItem(row, source);
                        if (campaign != null) {
                            results.add(campaign);
                            foundAny = true;
                        }
                    } catch (Exception e) {
                        log.warn("TOJOBCN 아이템 파싱 실패: {}", e.getMessage());
                    }
                }

                if (!foundAny) break;
                if (page < properties.getMaxPagesPerSite()) delayHandler.delay();
            } catch (Exception e) {
                log.error("TOJOBCN 페이지 {} 크롤링 실패: {}", page, e.getMessage());
                break;
            }
        }

        results = enricher.enrich(results, this::parseDetailPage);
        log.info("TOJOBCN 크롤링 완료: {}건", results.size());
        return results;
    }

    private CrawledCampaign parseDetailPage(CrawledCampaign campaign, Document doc) {
        // description from meta or gnuboard #bo_v_con
        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) description = metaDesc.attr("content");
        if (description == null || description.isBlank()) {
            Element viewCon = doc.selectFirst("#bo_v_con");
            if (viewCon != null) {
                String text = viewCon.text().trim();
                description = text.length() > 500 ? text.substring(0, 500) : text;
            }
        }

        // detailContent from gnuboard #bo_v_con or .view-content
        String detailContent = null;
        Element viewConEl = doc.selectFirst("#bo_v_con");
        if (viewConEl == null) viewConEl = doc.selectFirst(".view-content");
        if (viewConEl != null) detailContent = viewConEl.html();

        // reward from post body text containing "지원금" or "제공"
        String reward = null;
        String fullText = viewConEl != null ? viewConEl.text() : doc.text();
        Matcher rewardMatcher = Pattern.compile("지원금[:\\s]*([^\\n]{1,100})").matcher(fullText);
        if (rewardMatcher.find()) {
            reward = rewardMatcher.group(1).trim();
        }

        return new CrawledCampaign(
                campaign.getSourceCode(), campaign.getOriginalId(), campaign.getTitle(),
                coalesce(campaign.getDescription(), description),
                coalesce(campaign.getDetailContent(), detailContent),
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

    public CrawledCampaign parseItem(Element row, CrawlingSource source) {
        // 게시글 링크 추출 - a.item-subject 또는 wr_id 패턴
        Element link = row.selectFirst("a.item-subject");
        if (link == null) {
            link = row.selectFirst("a[href*=wr_id]");
        }
        if (link == null) return null;

        String href = link.attr("href");
        Matcher idMatcher = WR_ID_PATTERN.matcher(href);
        if (!idMatcher.find()) return null;
        String originalId = idMatcher.group(1);

        // 제목 추출
        String title = link.text().trim();
        // 상태 배지 제거 (모집중, 마감 등)
        title = title.replaceAll("^\\s*(모집중|마감|진행중)\\s*", "").trim();
        if (title.isEmpty() || title.length() < 3) return null;

        // 상태 판별
        CampaignStatus status = CampaignStatus.RECRUITING;
        String rowText = row.text();
        if (rowText.contains("마감")) {
            status = CampaignStatus.CLOSED;
        }

        // URL 생성
        String originalUrl = href.startsWith("http") ? href :
                BASE_URL + (href.startsWith("/") ? href : "/" + href);

        // 상태 배지 확인
        Element tackIcon = row.selectFirst("span.tack-icon");
        if (tackIcon != null) {
            String tackClass = tackIcon.className();
            if (tackClass.contains("bg-tojob_reviewer")) {
                // 리뷰어 상태는 마감이 아님
            }
        }

        String categoryText = "";

        CampaignCategory category = CategoryMapper.map(categoryText + " " + title);

        return new CrawledCampaign(
                source.getCode(), originalId, title, null, null,
                null, originalUrl, category, status,
                null, null, null, null,
                null, "블로그 리뷰 작성", null, "투잡커넥트,체험단"
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
                    source.getCode(), "tojobcn-" + i,
                    "[투잡커넥트] 체험단 캠페인 #" + i,
                    "투잡커넥트 체험단 설명 " + i, null,
                    "https://placehold.co/300x200?text=TOJOBCN+" + i,
                    BASE_URL + "/campaign/" + i,
                    cat, status,
                    3 + i % 6,
                    today.minusDays(3),
                    today.plusDays(5 + i), null,
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",투잡커넥트,체험단"
            ));
        }
        return mocks;
    }
}
