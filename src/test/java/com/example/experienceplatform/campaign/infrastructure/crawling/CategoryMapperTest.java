package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    @Test
    @DisplayName("맛집 → FOOD")
    void map_food() {
        assertThat(CategoryMapper.map("맛집")).isEqualTo(CampaignCategory.FOOD);
        assertThat(CategoryMapper.map("카페")).isEqualTo(CampaignCategory.FOOD);
    }

    @Test
    @DisplayName("뷰티 → BEAUTY")
    void map_beauty() {
        assertThat(CategoryMapper.map("뷰티")).isEqualTo(CampaignCategory.BEAUTY);
        assertThat(CategoryMapper.map("화장품")).isEqualTo(CampaignCategory.BEAUTY);
    }

    @Test
    @DisplayName("여행 → TRAVEL")
    void map_travel() {
        assertThat(CategoryMapper.map("여행")).isEqualTo(CampaignCategory.TRAVEL);
    }

    @Test
    @DisplayName("매핑 실패 → ETC")
    void map_unknown() {
        assertThat(CategoryMapper.map("알수없는카테고리")).isEqualTo(CampaignCategory.ETC);
    }

    @Test
    @DisplayName("null → ETC")
    void map_null() {
        assertThat(CategoryMapper.map(null)).isEqualTo(CampaignCategory.ETC);
    }

    @Test
    @DisplayName("빈 문자열 → ETC")
    void map_blank() {
        assertThat(CategoryMapper.map("")).isEqualTo(CampaignCategory.ETC);
    }
}
