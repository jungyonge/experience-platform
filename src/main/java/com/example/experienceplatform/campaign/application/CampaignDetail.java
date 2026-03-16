package com.example.experienceplatform.campaign.application;

import com.example.experienceplatform.campaign.domain.Campaign;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CampaignDetail {

    private final Long id;
    private final String sourceType;
    private final String sourceDisplayName;
    private final String title;
    private final String description;
    private final String detailContent;
    private final String thumbnailUrl;
    private final String originalUrl;
    private final String category;
    private final String categoryDisplayName;
    private final String status;
    private final String statusDisplayName;
    private final Integer recruitCount;
    private final LocalDate applyStartDate;
    private final LocalDate applyEndDate;
    private final LocalDate announcementDate;
    private final String reward;
    private final String mission;
    private final String address;
    private final List<String> keywords;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CampaignDetail(Long id, String sourceType, String sourceDisplayName,
                           String title, String description, String detailContent,
                           String thumbnailUrl, String originalUrl,
                           String category, String categoryDisplayName,
                           String status, String statusDisplayName,
                           Integer recruitCount, LocalDate applyStartDate,
                           LocalDate applyEndDate, LocalDate announcementDate,
                           String reward, String mission, String address,
                           List<String> keywords,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sourceType = sourceType;
        this.sourceDisplayName = sourceDisplayName;
        this.title = title;
        this.description = description;
        this.detailContent = detailContent;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.category = category;
        this.categoryDisplayName = categoryDisplayName;
        this.status = status;
        this.statusDisplayName = statusDisplayName;
        this.recruitCount = recruitCount;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.announcementDate = announcementDate;
        this.reward = reward;
        this.mission = mission;
        this.address = address;
        this.keywords = keywords;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CampaignDetail from(Campaign campaign) {
        return new CampaignDetail(
                campaign.getId(),
                campaign.getSourceCode(),
                campaign.getSourceName(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getDetailContent(),
                campaign.getThumbnailUrl(),
                campaign.getOriginalUrl(),
                campaign.getCategory().name(),
                campaign.getCategory().getDisplayName(),
                campaign.getStatus().name(),
                campaign.getStatus().getDisplayName(),
                campaign.getRecruitCount(),
                campaign.getApplyStartDate(),
                campaign.getApplyEndDate(),
                campaign.getAnnouncementDate(),
                campaign.getReward(),
                campaign.getMission(),
                campaign.getAddress(),
                campaign.getKeywordList(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }

    public Long getId() { return id; }
    public String getSourceType() { return sourceType; }
    public String getSourceDisplayName() { return sourceDisplayName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDetailContent() { return detailContent; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public String getCategory() { return category; }
    public String getCategoryDisplayName() { return categoryDisplayName; }
    public String getStatus() { return status; }
    public String getStatusDisplayName() { return statusDisplayName; }
    public Integer getRecruitCount() { return recruitCount; }
    public LocalDate getApplyStartDate() { return applyStartDate; }
    public LocalDate getApplyEndDate() { return applyEndDate; }
    public LocalDate getAnnouncementDate() { return announcementDate; }
    public String getReward() { return reward; }
    public String getMission() { return mission; }
    public String getAddress() { return address; }
    public List<String> getKeywords() { return keywords; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
