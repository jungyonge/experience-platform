package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingResult;

public class CrawlingResultResponse {

    private final String sourceType;
    private final String sourceDisplayName;
    private final String status;
    private final int totalCrawled;
    private final int newCount;
    private final int updatedCount;
    private final int failedCount;
    private final String errorMessage;
    private final long durationMs;

    private CrawlingResultResponse(String sourceType, String sourceDisplayName, String status,
                                   int totalCrawled, int newCount, int updatedCount,
                                   int failedCount, String errorMessage, long durationMs) {
        this.sourceType = sourceType;
        this.sourceDisplayName = sourceDisplayName;
        this.status = status;
        this.totalCrawled = totalCrawled;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public static CrawlingResultResponse from(CrawlingResult result) {
        return new CrawlingResultResponse(
                result.getSourceCode(),
                result.getSourceName(),
                result.getStatus().name(),
                result.getTotalCrawled(),
                result.getNewCount(),
                result.getUpdatedCount(),
                result.getFailedCount(),
                result.getErrorMessage(),
                result.getDurationMs()
        );
    }

    public String getSourceType() { return sourceType; }
    public String getSourceDisplayName() { return sourceDisplayName; }
    public String getStatus() { return status; }
    public int getTotalCrawled() { return totalCrawled; }
    public int getNewCount() { return newCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
}
