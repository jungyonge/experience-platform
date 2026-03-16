package com.example.experienceplatform.campaign.domain;

public enum CampaignCategory {

    FOOD("맛집"),
    BEAUTY("뷰티"),
    TRAVEL("여행/숙박"),
    LIFE("생활/가전"),
    DIGITAL("IT/디지털"),
    CULTURE("문화/도서"),
    ETC("기타");

    private final String displayName;

    CampaignCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
