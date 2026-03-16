package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.application.crawling.CrawlingSourceUpdateCommand;
import jakarta.validation.constraints.NotBlank;

public class CrawlingSourceUpdateRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "Base URL은 필수입니다.")
    private String baseUrl;

    private String listUrlPattern;
    private String description;

    @NotBlank(message = "크롤러 타입은 필수입니다.")
    private String crawlerType;

    private int displayOrder;

    public CrawlingSourceUpdateCommand toCommand() {
        return new CrawlingSourceUpdateCommand(name, baseUrl, listUrlPattern,
                description, crawlerType, displayOrder);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getListUrlPattern() { return listUrlPattern; }
    public void setListUrlPattern(String listUrlPattern) { this.listUrlPattern = listUrlPattern; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCrawlerType() { return crawlerType; }
    public void setCrawlerType(String crawlerType) { this.crawlerType = crawlerType; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
