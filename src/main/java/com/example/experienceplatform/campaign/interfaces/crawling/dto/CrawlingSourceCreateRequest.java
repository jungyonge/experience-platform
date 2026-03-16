package com.example.experienceplatform.campaign.interfaces.crawling.dto;

import com.example.experienceplatform.campaign.application.crawling.CrawlingSourceCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CrawlingSourceCreateRequest {

    @NotBlank(message = "코드는 필수입니다.")
    @Pattern(regexp = "^[A-Z0-9_]{2,30}$", message = "코드는 영문 대문자, 숫자, 언더스코어만 허용되며 2~30자여야 합니다.")
    private String code;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "Base URL은 필수입니다.")
    private String baseUrl;

    private String listUrlPattern;
    private String description;

    @NotBlank(message = "크롤러 타입은 필수입니다.")
    private String crawlerType;

    private int displayOrder;

    public CrawlingSourceCreateCommand toCommand() {
        return new CrawlingSourceCreateCommand(code, name, baseUrl, listUrlPattern,
                description, crawlerType, displayOrder);
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
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
