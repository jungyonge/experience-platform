package com.example.experienceplatform.campaign.application.crawling;

import com.example.experienceplatform.campaign.domain.CrawlingSource;

import java.time.LocalDateTime;

public class CrawlingSourceInfo {

    private final Long id;
    private final String code;
    private final String name;
    private final String baseUrl;
    private final String listUrlPattern;
    private final String description;
    private final String crawlerType;
    private final boolean active;
    private final int displayOrder;
    private final long campaignCount;
    private final LocalDateTime lastCrawledAt;
    private final LocalDateTime createdAt;

    public CrawlingSourceInfo(CrawlingSource source, long campaignCount, LocalDateTime lastCrawledAt) {
        this.id = source.getId();
        this.code = source.getCode();
        this.name = source.getName();
        this.baseUrl = source.getBaseUrl();
        this.listUrlPattern = source.getListUrlPattern();
        this.description = source.getDescription();
        this.crawlerType = source.getCrawlerType();
        this.active = source.isActive();
        this.displayOrder = source.getDisplayOrder();
        this.campaignCount = campaignCount;
        this.lastCrawledAt = lastCrawledAt;
        this.createdAt = source.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getListUrlPattern() { return listUrlPattern; }
    public String getDescription() { return description; }
    public String getCrawlerType() { return crawlerType; }
    public boolean isActive() { return active; }
    public int getDisplayOrder() { return displayOrder; }
    public long getCampaignCount() { return campaignCount; }
    public LocalDateTime getLastCrawledAt() { return lastCrawledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
