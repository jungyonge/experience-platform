package com.example.experienceplatform.campaign.application.crawling;

import com.example.experienceplatform.campaign.infrastructure.crawling.CrawledCampaign;

import java.util.List;

public class CrawlingTestResult {

    private final String sourceCode;
    private final String sourceName;
    private final String crawlerType;
    private final boolean success;
    private final int totalCount;
    private final String errorMessage;
    private final List<CrawledCampaign> items;

    private CrawlingTestResult(String sourceCode, String sourceName, String crawlerType,
                               boolean success, int totalCount, String errorMessage,
                               List<CrawledCampaign> items) {
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.crawlerType = crawlerType;
        this.success = success;
        this.totalCount = totalCount;
        this.errorMessage = errorMessage;
        this.items = items;
    }

    public static CrawlingTestResult success(String sourceCode, String sourceName,
                                              String crawlerType, List<CrawledCampaign> items) {
        return new CrawlingTestResult(sourceCode, sourceName, crawlerType,
                true, items.size(), null, items);
    }

    public static CrawlingTestResult failed(String sourceCode, String sourceName,
                                             String crawlerType, String errorMessage) {
        return new CrawlingTestResult(sourceCode, sourceName, crawlerType,
                false, 0, errorMessage, List.of());
    }

    public String getSourceCode() { return sourceCode; }
    public String getSourceName() { return sourceName; }
    public String getCrawlerType() { return crawlerType; }
    public boolean isSuccess() { return success; }
    public int getTotalCount() { return totalCount; }
    public String getErrorMessage() { return errorMessage; }
    public List<CrawledCampaign> getItems() { return items; }
}
