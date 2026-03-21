package com.example.experienceplatform.campaign.infrastructure.crawling.crawler;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.infrastructure.crawling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReviewExpeditionCrawler implements CampaignCrawler {

    private static final Logger log = LoggerFactory.getLogger(ReviewExpeditionCrawler.class);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;
    private final RobotsTxtChecker robotsTxtChecker;
    private final CrawlingDelayHandler delayHandler;

    public ReviewExpeditionCrawler(CrawlingProperties properties, JsoupClient jsoupClient,
                                   RobotsTxtChecker robotsTxtChecker, CrawlingDelayHandler delayHandler) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
        this.robotsTxtChecker = robotsTxtChecker;
        this.delayHandler = delayHandler;
    }

    @Override
    public String getCrawlerType() {
        return "REVIEW_EXPEDITION";
    }

    @Override
    public List<CrawledCampaign> crawl(CrawlingSource source) {
        if (properties.isMockEnabled()) {
            return generateMockData(source);
        }
        return crawlReal(source);
    }

    private List<CrawledCampaign> crawlReal(CrawlingSource source) {
        // reviewexpedition.co.kr DNS 조회 실패 - 사이트가 폐쇄되었거나 도메인이 만료된 상태입니다.
        log.warn("REVIEW_EXPEDITION 사이트(reviewexpedition.co.kr)에 접근할 수 없습니다 (DNS 조회 실패). 빈 결과를 반환합니다.");
        return List.of();
    }

    private List<CrawledCampaign> generateMockData(CrawlingSource source) {
        List<CrawledCampaign> mocks = new ArrayList<>();
        CampaignCategory[] categories = {CampaignCategory.FOOD, CampaignCategory.BEAUTY, CampaignCategory.LIFE,
                CampaignCategory.DIGITAL, CampaignCategory.TRAVEL};
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= properties.getMockCount(); i++) {
            CampaignCategory cat = categories[(i - 1) % categories.length];
            CampaignStatus status = i <= 10 ? CampaignStatus.RECRUITING : CampaignStatus.CLOSED;
            mocks.add(new CrawledCampaign(
                    source.getCode(), "review-expedition-" + i,
                    "[리뷰원정대] 체험단 캠페인 #" + i,
                    "리뷰원정대 체험단 설명 " + i,
                    "리뷰원정대 상세 내용 " + i,
                    "https://placehold.co/300x200?text=REVIEW_EXPEDITION+" + i,
                    source.getBaseUrl() + "/campaign/review-expedition-" + i,
                    cat, status,
                    5 + i % 8,
                    today.minusDays(3),
                    today.plusDays(7 + i),
                    today.plusDays(12 + i),
                    "제공 내역 " + i,
                    "블로그 리뷰 작성",
                    null,
                    cat.getDisplayName() + ",리뷰원정대,체험단"
            ));
        }
        return mocks;
    }
}
