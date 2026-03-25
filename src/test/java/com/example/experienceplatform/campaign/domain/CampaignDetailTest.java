package com.example.experienceplatform.campaign.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignDetailTest {

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);

    @Test
    @DisplayName("상세 필드 포함 생성자로 Campaign 생성")
    void create_campaign_with_detail_fields() {
        Campaign campaign = new Campaign(
                REVU_SOURCE, "1001",
                "테스트 캠페인", "설명", "https://thumb.jpg",
                "https://revu.net/1001",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 3),
                "상세 설명 텍스트", "2인 식사권", "블로그 리뷰 작성",
                "서울 강남구 역삼동 123", "강남맛집,이탈리안,파스타");

        assertThat(campaign.getDetailContent()).isEqualTo("상세 설명 텍스트");
        assertThat(campaign.getReward()).isEqualTo("2인 식사권");
        assertThat(campaign.getMission()).isEqualTo("블로그 리뷰 작성");
        assertThat(campaign.getAddress()).isEqualTo("서울 강남구 역삼동 123");
        assertThat(campaign.getKeywords()).isEqualTo("강남맛집,이탈리안,파스타");
    }

    @Test
    @DisplayName("getKeywordList - 정상 파싱")
    void getKeywordList_normal() {
        Campaign campaign = createCampaignWithKeywords("강남맛집,이탈리안,파스타");

        List<String> result = campaign.getKeywordList();

        assertThat(result).containsExactly("강남맛집", "이탈리안", "파스타");
    }

    @Test
    @DisplayName("getKeywordList - null → 빈 리스트")
    void getKeywordList_null() {
        Campaign campaign = createCampaignWithKeywords(null);

        List<String> result = campaign.getKeywordList();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getKeywordList - 빈 문자열 → 빈 리스트")
    void getKeywordList_empty() {
        Campaign campaign = createCampaignWithKeywords("");

        List<String> result = campaign.getKeywordList();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getKeywordList - 공백 문자열 → 빈 리스트")
    void getKeywordList_blank() {
        Campaign campaign = createCampaignWithKeywords("   ");

        List<String> result = campaign.getKeywordList();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getKeywordList - trim + 빈 값 제거 + 순서 유지")
    void getKeywordList_trimAndFilter() {
        Campaign campaign = createCampaignWithKeywords(" 강남맛집 ,  이탈리안 , ");

        List<String> result = campaign.getKeywordList();

        assertThat(result).containsExactly("강남맛집", "이탈리안");
    }

    @Test
    @DisplayName("update() 확장 필드 갱신 확인")
    void update_with_detail_fields() {
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
                "수정된 상세 설명", "수정된 제공내역", "수정된 미션",
                "수정된 주소", "수정키워드1,수정키워드2", null, null);

        assertThat(campaign.getTitle()).isEqualTo("수정된 제목");
        assertThat(campaign.getDetailContent()).isEqualTo("수정된 상세 설명");
        assertThat(campaign.getReward()).isEqualTo("수정된 제공내역");
        assertThat(campaign.getMission()).isEqualTo("수정된 미션");
        assertThat(campaign.getAddress()).isEqualTo("수정된 주소");
        assertThat(campaign.getKeywords()).isEqualTo("수정키워드1,수정키워드2");
        assertThat(campaign.getKeywordList()).containsExactly("수정키워드1", "수정키워드2");
    }

    private Campaign createCampaignWithKeywords(String keywords) {
        return new Campaign(
                REVU_SOURCE, "test-1",
                "테스트", null, null,
                "https://example.com",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, null, null,
                null, null, null, null, keywords);
    }
}
