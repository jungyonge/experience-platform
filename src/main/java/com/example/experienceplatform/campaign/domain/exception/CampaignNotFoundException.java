package com.example.experienceplatform.campaign.domain.exception;

import com.example.experienceplatform.member.domain.exception.BusinessException;

public class CampaignNotFoundException extends BusinessException {

    public CampaignNotFoundException(Long campaignId) {
        super("CAMPAIGN_NOT_FOUND", "캠페인을 찾을 수 없습니다. id=" + campaignId);
    }
}
