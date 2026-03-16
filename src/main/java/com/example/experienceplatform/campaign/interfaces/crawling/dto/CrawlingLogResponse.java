package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;

import java.time.LocalDateTime;

public class CrawlingLogResponse {

    private final Long id;
    private final String sourceType;
    private final String sourceDisplayName;
    private final String status;
    private final int totalCrawled;
    private final int newCount;
    private final int updatedCount;
    private final int failedCount;
    private final String errorMessage;
    private final long durationMs;
    private final LocalDateTime executedAt;

    private CrawlingLogResponse(Long id, String sourceType, String sourceDisplayName, String status,
                                int totalCrawled, int newCount, int updatedCount, int failedCount,
                                String errorMessage, long durationMs, LocalDateTime executedAt) {
        this.id = id;
        this.sourceType = sourceType;
        this.sourceDisplayName = sourceDisplayName;
        this.status = status;
        this.totalCrawled = totalCrawled;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.executedAt = executedAt;
    }

    public static CrawlingLogResponse from(CrawlingLog log) {
        return new CrawlingLogResponse(
                log.getId(),
                log.getSourceCode(),
                log.getSourceName(),
                log.getStatus().name(),
                log.getTotalCrawled(),
                log.getNewCount(),
                log.getUpdatedCount(),
                log.getFailedCount(),
                log.getErrorMessage(),
                log.getDurationMs(),
                log.getExecutedAt()
        );
    }

    public Long getId() { return id; }
    public String getSourceType() { return sourceType; }
    public String getSourceDisplayName() { return sourceDisplayName; }
    public String getStatus() { return status; }
    public int getTotalCrawled() { return totalCrawled; }
    public int getNewCount() { return newCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
