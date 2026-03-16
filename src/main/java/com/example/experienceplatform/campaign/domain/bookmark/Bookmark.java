package com.example.experienceplatform.campaign.domain.bookmark;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_bookmark_member_campaign",
                columnNames = {"member_id", "campaign_id"}
        ))
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Bookmark() {
    }

    public Bookmark(Long memberId, Long campaignId) {
        this.memberId = memberId;
        this.campaignId = campaignId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getCampaignId() { return campaignId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
