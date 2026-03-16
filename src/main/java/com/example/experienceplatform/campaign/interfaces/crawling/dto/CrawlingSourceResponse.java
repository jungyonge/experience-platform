package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.application.crawling.CrawlingSourceInfo;

import java.time.LocalDateTime;

public class CrawlingSourceResponse {

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

    private CrawlingSourceResponse(Long id, String code, String name, String baseUrl,
                                   String listUrlPattern, String description, String crawlerType,
                                   boolean active, int displayOrder, long campaignCount,
                                   LocalDateTime lastCrawledAt, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.baseUrl = baseUrl;
        this.listUrlPattern = listUrlPattern;
        this.description = description;
        this.crawlerType = crawlerType;
        this.active = active;
        this.displayOrder = displayOrder;
        this.campaignCount = campaignCount;
        this.lastCrawledAt = lastCrawledAt;
        this.createdAt = createdAt;
    }

    public static CrawlingSourceResponse from(CrawlingSourceInfo info) {
        return new CrawlingSourceResponse(
                info.getId(), info.getCode(), info.getName(), info.getBaseUrl(),
                info.getListUrlPattern(), info.getDescription(), info.getCrawlerType(),
                info.isActive(), info.getDisplayOrder(), info.getCampaignCount(),
                info.getLastCrawledAt(), info.getCreatedAt());
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
