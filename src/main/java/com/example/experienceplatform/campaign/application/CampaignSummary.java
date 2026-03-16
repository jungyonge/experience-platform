package com.example.experienceplatform.campaign.application;

import com.example.experienceplatform.campaign.domain.Campaign;

import java.time.LocalDate;

public class CampaignSummary {

    private final Long id;
    private final String sourceType;
    private final String sourceDisplayName;
    private final String title;
    private final String thumbnailUrl;
    private final String originalUrl;
    private final String category;
    private final String categoryDisplayName;
    private final String status;
    private final String statusDisplayName;
    private final Integer recruitCount;
    private final LocalDate applyEndDate;

    private CampaignSummary(Long id, String sourceType, String sourceDisplayName,
                            String title, String thumbnailUrl, String originalUrl,
                            String category, String categoryDisplayName,
                            String status, String statusDisplayName,
                            Integer recruitCount, LocalDate applyEndDate) {
        this.id = id;
        this.sourceType = sourceType;
        this.sourceDisplayName = sourceDisplayName;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.category = category;
        this.categoryDisplayName = categoryDisplayName;
        this.status = status;
        this.statusDisplayName = statusDisplayName;
        this.recruitCount = recruitCount;
        this.applyEndDate = applyEndDate;
    }

    public static CampaignSummary from(Campaign campaign) {
        return new CampaignSummary(
                campaign.getId(),
                campaign.getSourceCode(),
                campaign.getSourceName(),
                campaign.getTitle(),
                campaign.getThumbnailUrl(),
                campaign.getOriginalUrl(),
                campaign.getCategory().name(),
                campaign.getCategory().getDisplayName(),
                campaign.getStatus().name(),
                campaign.getStatus().getDisplayName(),
                campaign.getRecruitCount(),
                campaign.getApplyEndDate()
        );
    }

    public Long getId() { return id; }
    public String getSourceType() { return sourceType; }
    public String getSourceDisplayName() { return sourceDisplayName; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public String getCategory() { return category; }
    public String getCategoryDisplayName() { return categoryDisplayName; }
    public String getStatus() { return status; }
    public String getStatusDisplayName() { return statusDisplayName; }
    public Integer getRecruitCount() { return recruitCount; }
    public LocalDate getApplyEndDate() { return applyEndDate; }
}
