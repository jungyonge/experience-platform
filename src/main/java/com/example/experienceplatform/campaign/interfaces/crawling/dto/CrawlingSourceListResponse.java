package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.application.crawling.CrawlingSourceInfo;

import java.util.List;

public class CrawlingSourceListResponse {

    private final List<CrawlingSourceResponse> sources;
    private final List<String> availableCrawlerTypes;

    public CrawlingSourceListResponse(List<CrawlingSourceResponse> sources,
                                      List<String> availableCrawlerTypes) {
        this.sources = sources;
        this.availableCrawlerTypes = availableCrawlerTypes;
    }

    public static CrawlingSourceListResponse from(List<CrawlingSourceInfo> infos,
                                                   List<String> crawlerTypes) {
        List<CrawlingSourceResponse> responses = infos.stream()
                .map(CrawlingSourceResponse::from)
                .toList();
        return new CrawlingSourceListResponse(responses, crawlerTypes);
    }

    public List<CrawlingSourceResponse> getSources() { return sources; }
    public List<String> getAvailableCrawlerTypes() { return availableCrawlerTypes; }
}
