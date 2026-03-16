package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogStatus;

public class CrawlingResult {

    private final String sourceCode;
    private final String sourceName;
    private final CrawlingLogStatus status;
    private final int totalCrawled;
    private final int newCount;
    private final int updatedCount;
    private final int failedCount;
    private final String errorMessage;
    private final long durationMs;

    public CrawlingResult(String sourceCode, String sourceName, CrawlingLogStatus status,
                          int totalCrawled, int newCount, int updatedCount,
                          int failedCount, String errorMessage, long durationMs) {
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.status = status;
        this.totalCrawled = totalCrawled;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public static CrawlingResult success(String sourceCode, String sourceName,
                                          int totalCrawled, int newCount, int updatedCount,
                                          int failedCount, long durationMs) {
        CrawlingLogStatus status = failedCount > 0 ? CrawlingLogStatus.PARTIAL : CrawlingLogStatus.SUCCESS;
        return new CrawlingResult(sourceCode, sourceName, status, totalCrawled, newCount, updatedCount, failedCount, null, durationMs);
    }

    public static CrawlingResult failed(String sourceCode, String sourceName,
                                         String errorMessage, long durationMs) {
        return new CrawlingResult(sourceCode, sourceName, CrawlingLogStatus.FAILED, 0, 0, 0, 0, errorMessage, durationMs);
    }

    public String getSourceCode() { return sourceCode; }
    public String getSourceName() { return sourceName; }
    public CrawlingLogStatus getStatus() { return status; }
    public int getTotalCrawled() { return totalCrawled; }
    public int getNewCount() { return newCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
}
