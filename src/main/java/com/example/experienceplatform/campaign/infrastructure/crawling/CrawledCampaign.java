package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;

import java.time.LocalDate;

public class CrawledCampaign {

    private final String sourceCode;
    private final String originalId;
    private final String title;
    private final String description;
    private final String detailContent;
    private final String thumbnailUrl;
    private final String originalUrl;
    private final CampaignCategory category;
    private final CampaignStatus status;
    private final Integer recruitCount;
    private final LocalDate applyStartDate;
    private final LocalDate applyEndDate;
    private final LocalDate announcementDate;
    private final String reward;
    private final String mission;
    private final String address;
    private final String keywords;
    private final Integer currentApplicants;

    public CrawledCampaign(String sourceCode, String originalId, String title,
                           String description, String detailContent, String thumbnailUrl,
                           String originalUrl, CampaignCategory category, CampaignStatus status,
                           Integer recruitCount, LocalDate applyStartDate, LocalDate applyEndDate,
                           LocalDate announcementDate, String reward, String mission,
                           String address, String keywords) {
        this(sourceCode, originalId, title, description, detailContent, thumbnailUrl,
                originalUrl, category, status, recruitCount, applyStartDate, applyEndDate,
                announcementDate, reward, mission, address, keywords, null);
    }

    public CrawledCampaign(String sourceCode, String originalId, String title,
                           String description, String detailContent, String thumbnailUrl,
                           String originalUrl, CampaignCategory category, CampaignStatus status,
                           Integer recruitCount, LocalDate applyStartDate, LocalDate applyEndDate,
                           LocalDate announcementDate, String reward, String mission,
                           String address, String keywords, Integer currentApplicants) {
        this.sourceCode = sourceCode;
        this.originalId = originalId;
        this.title = title;
        this.description = description;
        this.detailContent = detailContent;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.category = category;
        this.status = status;
        this.recruitCount = recruitCount;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.announcementDate = announcementDate;
        this.reward = reward;
        this.mission = mission;
        this.address = address;
        this.keywords = keywords;
        this.currentApplicants = currentApplicants;
    }

    public String getSourceCode() { return sourceCode; }
    public String getOriginalId() { return originalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDetailContent() { return detailContent; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public CampaignCategory getCategory() { return category; }
    public CampaignStatus getStatus() { return status; }
    public Integer getRecruitCount() { return recruitCount; }
    public LocalDate getApplyStartDate() { return applyStartDate; }
    public LocalDate getApplyEndDate() { return applyEndDate; }
    public LocalDate getAnnouncementDate() { return announcementDate; }
    public String getReward() { return reward; }
    public String getMission() { return mission; }
    public String getAddress() { return address; }
    public String getKeywords() { return keywords; }
    public Integer getCurrentApplicants() { return currentApplicants; }
}
