package com.example.experienceplatform.campaign.infrastructure.crawling.log;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawling_logs")
public class CrawlingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String sourceCode;

    @Column(nullable = false, length = 50)
    private String sourceName;

    @Column(length = 50)
    private String crawlerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlingLogStatus status;

    private int totalCrawled;

    private int newCount;

    private int updatedCount;

    private int failedCount;

    @Column(length = 1000)
    private String errorMessage;

    private long durationMs;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    protected CrawlingLog() {
    }

    public CrawlingLog(String sourceCode, String sourceName, String crawlerType,
                       CrawlingLogStatus status, int totalCrawled, int newCount,
                       int updatedCount, int failedCount, String errorMessage, long durationMs) {
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.crawlerType = crawlerType;
        this.status = status;
        this.totalCrawled = totalCrawled;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.executedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSourceCode() { return sourceCode; }
    public String getSourceName() { return sourceName; }
    public String getCrawlerType() { return crawlerType; }
    public CrawlingLogStatus getStatus() { return status; }
    public int getTotalCrawled() { return totalCrawled; }
    public int getNewCount() { return newCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
