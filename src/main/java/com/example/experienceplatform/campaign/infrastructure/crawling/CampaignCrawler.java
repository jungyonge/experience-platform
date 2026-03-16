package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CrawlingSource;

import java.util.List;

public interface CampaignCrawler {
    String getCrawlerType();
    List<CrawledCampaign> crawl(CrawlingSource source);
}
