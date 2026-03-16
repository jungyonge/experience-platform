package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CampaignCategory;

import java.util.Map;

public class CategoryMapper {

    private static final Map<String, CampaignCategory> MAPPINGS = Map.ofEntries(
            Map.entry("맛집", CampaignCategory.FOOD),
            Map.entry("음식", CampaignCategory.FOOD),
            Map.entry("식품", CampaignCategory.FOOD),
            Map.entry("카페", CampaignCategory.FOOD),
            Map.entry("레스토랑", CampaignCategory.FOOD),
            Map.entry("food", CampaignCategory.FOOD),
            Map.entry("뷰티", CampaignCategory.BEAUTY),
            Map.entry("화장품", CampaignCategory.BEAUTY),
            Map.entry("미용", CampaignCategory.BEAUTY),
            Map.entry("beauty", CampaignCategory.BEAUTY),
            Map.entry("여행", CampaignCategory.TRAVEL),
            Map.entry("숙박", CampaignCategory.TRAVEL),
            Map.entry("호텔", CampaignCategory.TRAVEL),
            Map.entry("travel", CampaignCategory.TRAVEL),
            Map.entry("생활", CampaignCategory.LIFE),
            Map.entry("가전", CampaignCategory.LIFE),
            Map.entry("리빙", CampaignCategory.LIFE),
            Map.entry("life", CampaignCategory.LIFE),
            Map.entry("디지털", CampaignCategory.DIGITAL),
            Map.entry("IT", CampaignCategory.DIGITAL),
            Map.entry("전자", CampaignCategory.DIGITAL),
            Map.entry("digital", CampaignCategory.DIGITAL),
            Map.entry("문화", CampaignCategory.CULTURE),
            Map.entry("도서", CampaignCategory.CULTURE),
            Map.entry("공연", CampaignCategory.CULTURE),
            Map.entry("culture", CampaignCategory.CULTURE)
    );

    private CategoryMapper() {
    }

    public static CampaignCategory map(String categoryText) {
        if (categoryText == null || categoryText.isBlank()) {
            return CampaignCategory.ETC;
        }
        String trimmed = categoryText.trim().toLowerCase();
        for (Map.Entry<String, CampaignCategory> entry : MAPPINGS.entrySet()) {
            if (trimmed.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return CampaignCategory.ETC;
    }
}
