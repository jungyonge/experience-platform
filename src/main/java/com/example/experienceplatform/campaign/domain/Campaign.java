package com.example.experienceplatform.campaign.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "campaigns",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_campaign_source_original_id",
                columnNames = {"crawling_source_id", "original_id"}
        ))
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawling_source_id", nullable = false)
    private CrawlingSource crawlingSource;

    @Column(name = "original_id", nullable = false)
    private String originalId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    private String thumbnailUrl;

    @Column(nullable = false)
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    private Integer recruitCount;

    private LocalDate applyStartDate;

    private LocalDate applyEndDate;

    private LocalDate announcementDate;

    @Lob
    @Column(length = 5000)
    private String detailContent;

    @Column(length = 500)
    private String reward;

    @Column(length = 500)
    private String mission;

    @Column(length = 300)
    private String address;

    @Column(length = 500)
    private String keywords;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Campaign() {
    }

    public Campaign(CrawlingSource crawlingSource, String originalId, String title,
                    String description, String thumbnailUrl, String originalUrl,
                    CampaignCategory category, CampaignStatus status,
                    Integer recruitCount, LocalDate applyStartDate,
                    LocalDate applyEndDate, LocalDate announcementDate) {
        this.crawlingSource = crawlingSource;
        this.originalId = originalId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.category = category;
        this.status = status;
        this.recruitCount = recruitCount;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.announcementDate = announcementDate;
    }

    public Campaign(CrawlingSource crawlingSource, String originalId, String title,
                    String description, String thumbnailUrl, String originalUrl,
                    CampaignCategory category, CampaignStatus status,
                    Integer recruitCount, LocalDate applyStartDate,
                    LocalDate applyEndDate, LocalDate announcementDate,
                    String detailContent, String reward, String mission,
                    String address, String keywords) {
        this.crawlingSource = crawlingSource;
        this.originalId = originalId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.category = category;
        this.status = status;
        this.recruitCount = recruitCount;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.announcementDate = announcementDate;
        this.detailContent = detailContent;
        this.reward = reward;
        this.mission = mission;
        this.address = address;
        this.keywords = keywords;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String description, String thumbnailUrl,
                       String originalUrl, CampaignCategory category,
                       CampaignStatus status, Integer recruitCount,
                       LocalDate applyStartDate, LocalDate applyEndDate,
                       LocalDate announcementDate, String detailContent,
                       String reward, String mission, String address,
                       String keywords) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.originalUrl = originalUrl;
        this.category = category;
        this.status = status;
        this.recruitCount = recruitCount;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.announcementDate = announcementDate;
        this.detailContent = detailContent;
        this.reward = reward;
        this.mission = mission;
        this.address = address;
        this.keywords = keywords;
    }

    public String getSourceCode() {
        return crawlingSource.getCode();
    }

    public String getSourceName() {
        return crawlingSource.getName();
    }

    public List<String> getKeywordList() {
        if (keywords == null || keywords.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public CrawlingSource getCrawlingSource() { return crawlingSource; }
    public String getOriginalId() { return originalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public CampaignCategory getCategory() { return category; }
    public CampaignStatus getStatus() { return status; }
    public Integer getRecruitCount() { return recruitCount; }
    public LocalDate getApplyStartDate() { return applyStartDate; }
    public LocalDate getApplyEndDate() { return applyEndDate; }
    public LocalDate getAnnouncementDate() { return announcementDate; }
    public String getDetailContent() { return detailContent; }
    public String getReward() { return reward; }
    public String getMission() { return mission; }
    public String getAddress() { return address; }
    public String getKeywords() { return keywords; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
