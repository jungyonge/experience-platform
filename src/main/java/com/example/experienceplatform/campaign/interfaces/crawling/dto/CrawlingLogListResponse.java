package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;

import java.util.List;

public class CrawlingLogListResponse {

    private final List<CrawlingLogResponse> logs;

    public CrawlingLogListResponse(List<CrawlingLogResponse> logs) {
        this.logs = logs;
    }

    public static CrawlingLogListResponse from(List<CrawlingLog> logs) {
        return new CrawlingLogListResponse(
                logs.stream().map(CrawlingLogResponse::from).toList()
        );
    }

    public List<CrawlingLogResponse> getLogs() { return logs; }
}
