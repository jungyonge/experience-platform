package com.example.experienceplatform.campaign.application.crawling;

public class CrawlingSourceCreateCommand {

    private final String code;
    private final String name;
    private final String baseUrl;
    private final String listUrlPattern;
    private final String description;
    private final String crawlerType;
    private final int displayOrder;

    public CrawlingSourceCreateCommand(String code, String name, String baseUrl,
                                       String listUrlPattern, String description,
                                       String crawlerType, int displayOrder) {
        this.code = code;
        this.name = name;
        this.baseUrl = baseUrl;
        this.listUrlPattern = listUrlPattern;
        this.description = description;
        this.crawlerType = crawlerType;
        this.displayOrder = displayOrder;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getListUrlPattern() { return listUrlPattern; }
    public String getDescription() { return description; }
    public String getCrawlerType() { return crawlerType; }
    public int getDisplayOrder() { return displayOrder; }
}
