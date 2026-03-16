package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.application.crawling.CrawlingTestResult;
import com.example.experienceplatform.campaign.infrastructure.crawling.CrawledCampaign;

import java.util.List;

public class CrawlingTestResponse {

    private final String sourceCode;
    private final String sourceName;
    private final String crawlerType;
    private final boolean success;
    private final int totalCount;
    private final String errorMessage;
    private final List<TestItem> items;

    private CrawlingTestResponse(String sourceCode, String sourceName, String crawlerType,
                                 boolean success, int totalCount, String errorMessage,
                                 List<TestItem> items) {
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.crawlerType = crawlerType;
        this.success = success;
        this.totalCount = totalCount;
        this.errorMessage = errorMessage;
        this.items = items;
    }

    public static CrawlingTestResponse from(CrawlingTestResult result) {
        List<TestItem> items = result.getItems().stream()
                .map(TestItem::from)
                .toList();
        return new CrawlingTestResponse(
                result.getSourceCode(), result.getSourceName(), result.getCrawlerType(),
                result.isSuccess(), result.getTotalCount(), result.getErrorMessage(), items);
    }

    public String getSourceCode() { return sourceCode; }
    public String getSourceName() { return sourceName; }
    public String getCrawlerType() { return crawlerType; }
    public boolean isSuccess() { return success; }
    public int getTotalCount() { return totalCount; }
    public String getErrorMessage() { return errorMessage; }
    public List<TestItem> getItems() { return items; }

    public static class TestItem {
        private final String originalId;
        private final String title;
        private final String originalUrl;
        private final String thumbnailUrl;
        private final String category;

        private TestItem(String originalId, String title, String originalUrl,
                         String thumbnailUrl, String category) {
            this.originalId = originalId;
            this.title = title;
            this.originalUrl = originalUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.category = category;
        }

        public static TestItem from(CrawledCampaign item) {
            return new TestItem(
                    item.getOriginalId(), item.getTitle(), item.getOriginalUrl(),
                    item.getThumbnailUrl(),
                    item.getCategory() != null ? item.getCategory().name() : null);
        }

        public String getOriginalId() { return originalId; }
        public String getTitle() { return title; }
        public String getOriginalUrl() { return originalUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public String getCategory() { return category; }
    }
}
