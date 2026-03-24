package com.example.experienceplatform.campaign.domain;

import java.util.Set;

public class CampaignSearchCondition {

    private final String keyword;
    private final Set<String> sourceCodes;
    private final Set<CampaignCategory> categories;
    private final CampaignStatus status;
    private final String region;

    public CampaignSearchCondition(String keyword, Set<String> sourceCodes,
                                   Set<CampaignCategory> categories, CampaignStatus status,
                                   String region) {
        this.keyword = keyword;
        this.sourceCodes = sourceCodes;
        this.categories = categories;
        this.status = status;
        this.region = region;
    }

    public String getKeyword() {
        return keyword;
    }

    public Set<String> getSourceCodes() {
        return sourceCodes;
    }

    public Set<CampaignCategory> getCategories() {
        return categories;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public String getRegion() {
        return region;
    }
}
