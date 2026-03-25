package com.example.experienceplatform.campaign.domain;

import java.util.Set;

public class CampaignSearchCondition {

    private final String keyword;
    private final Set<String> sourceCodes;
    private final Set<CampaignCategory> categories;
    private final CampaignStatus status;
    private final Long regionId;
    private final String sido;

    public CampaignSearchCondition(String keyword, Set<String> sourceCodes,
                                   Set<CampaignCategory> categories, CampaignStatus status,
                                   Long regionId, String sido) {
        this.keyword = keyword;
        this.sourceCodes = sourceCodes;
        this.categories = categories;
        this.status = status;
        this.regionId = regionId;
        this.sido = sido;
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

    public Long getRegionId() {
        return regionId;
    }

    public String getSido() {
        return sido;
    }
}
