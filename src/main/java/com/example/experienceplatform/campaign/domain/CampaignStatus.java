package com.example.experienceplatform.campaign.domain;

public enum CampaignStatus {

    RECRUITING("모집중"),
    CLOSED("모집마감"),
    ANNOUNCED("발표완료"),
    COMPLETED("종료");

    private final String displayName;

    CampaignStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
