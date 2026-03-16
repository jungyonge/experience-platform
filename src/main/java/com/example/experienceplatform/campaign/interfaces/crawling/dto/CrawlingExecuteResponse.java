package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingResult;

import java.time.LocalDateTime;
import java.util.List;

public class CrawlingExecuteResponse {

    private final List<CrawlingResultResponse> results;
    private final long totalDurationMs;
    private final LocalDateTime executedAt;

    public CrawlingExecuteResponse(List<CrawlingResultResponse> results, long totalDurationMs) {
        this.results = results;
        this.totalDurationMs = totalDurationMs;
        this.executedAt = LocalDateTime.now();
    }

    public static CrawlingExecuteResponse from(List<CrawlingResult> results) {
        List<CrawlingResultResponse> responses = results.stream()
                .map(CrawlingResultResponse::from)
                .toList();
        long totalDuration = results.stream().mapToLong(CrawlingResult::getDurationMs).sum();
        return new CrawlingExecuteResponse(responses, totalDuration);
    }

    public List<CrawlingResultResponse> getResults() { return results; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
