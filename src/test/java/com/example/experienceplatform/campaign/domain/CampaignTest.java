package com.example.experienceplatform.campaign.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignTest {

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);
    private static final CrawlingSource MBLE_SOURCE =
            new CrawlingSource("MBLE", "미블", "https://www.mble.xyz", null, null, "MBLE", 2);
    private static final CrawlingSource GANGNAM_SOURCE =
            new CrawlingSource("GANGNAM", "강남맛집", "https://www.gangnam.kr", null, null, "GANGNAM", 3);

    @Test
    @DisplayName("Campaign 생성 시 필드가 올바르게 설정됨")
    void create_campaign() {
        Campaign campaign = new Campaign(
                REVU_SOURCE, "1001",
                "테스트 캠페인", "설명", "https://thumb.jpg",
                "https://revu.net/1001",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        assertThat(campaign.getSourceCode()).isEqualTo("REVU");
        assertThat(campaign.getOriginalId()).isEqualTo("1001");
        assertThat(campaign.getTitle()).isEqualTo("테스트 캠페인");
        assertThat(campaign.getCategory()).isEqualTo(CampaignCategory.FOOD);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RECRUITING);
        assertThat(campaign.getRecruitCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("update() 메서드로 필드 갱신")
    void update_campaign() {
        Campaign campaign = new Campaign(
                REVU_SOURCE, "1001",
                "원래 제목", null, null,
                "https://revu.net/1001",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, null, null);

        campaign.update("수정된 제목", "수정된 설명", "https://new-thumb.jpg",
                "https://revu.net/1001-new",
                CampaignCategory.BEAUTY, CampaignStatus.CLOSED,
                10, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 5),
                null, null, null, null, null);

        assertThat(campaign.getTitle()).isEqualTo("수정된 제목");
        assertThat(campaign.getDescription()).isEqualTo("수정된 설명");
        assertThat(campaign.getCategory()).isEqualTo(CampaignCategory.BEAUTY);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(campaign.getRecruitCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("CrawlingSource code/name 매핑")
    void crawlingSource_codeAndName() {
        assertThat(REVU_SOURCE.getCode()).isEqualTo("REVU");
        assertThat(REVU_SOURCE.getName()).isEqualTo("레뷰");
        assertThat(MBLE_SOURCE.getCode()).isEqualTo("MBLE");
        assertThat(MBLE_SOURCE.getName()).isEqualTo("미블");
        assertThat(GANGNAM_SOURCE.getCode()).isEqualTo("GANGNAM");
        assertThat(GANGNAM_SOURCE.getName()).isEqualTo("강남맛집");
    }

    @Test
    @DisplayName("CampaignCategory displayName 매핑")
    void category_displayName() {
        assertThat(CampaignCategory.FOOD.getDisplayName()).isEqualTo("맛집");
        assertThat(CampaignCategory.BEAUTY.getDisplayName()).isEqualTo("뷰티");
        assertThat(CampaignCategory.TRAVEL.getDisplayName()).isEqualTo("여행/숙박");
        assertThat(CampaignCategory.LIFE.getDisplayName()).isEqualTo("생활/가전");
        assertThat(CampaignCategory.DIGITAL.getDisplayName()).isEqualTo("IT/디지털");
        assertThat(CampaignCategory.CULTURE.getDisplayName()).isEqualTo("문화/도서");
        assertThat(CampaignCategory.ETC.getDisplayName()).isEqualTo("기타");
    }

    @Test
    @DisplayName("CampaignSearchCondition 빈 조건 생성")
    void searchCondition_empty() {
        CampaignSearchCondition condition = new CampaignSearchCondition(null, null, null, null);

        assertThat(condition.getKeyword()).isNull();
        assertThat(condition.getSourceCodes()).isNull();
        assertThat(condition.getCategories()).isNull();
        assertThat(condition.getStatus()).isNull();
    }
}
