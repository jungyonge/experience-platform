package com.example.experienceplatform.campaign.application;

import com.example.experienceplatform.campaign.domain.*;
import com.example.experienceplatform.campaign.domain.exception.CampaignNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceDetailTest {

    @InjectMocks
    private CampaignService campaignService;

    @Mock
    private CampaignRepository campaignRepository;

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);
    private static final CrawlingSource GANGNAM_SOURCE =
            new CrawlingSource("GANGNAM", "강남맛집", "https://www.gangnam.kr", null, null, "GANGNAM", 2);

    @Test
    @DisplayName("정상 상세 조회")
    void getDetail_success() {
        Campaign campaign = new Campaign(
                REVU_SOURCE, "1001",
                "테스트 캠페인", "설명", "https://thumb.jpg",
                "https://revu.net/1001",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 3),
                "상세 설명", "2인 식사권", "블로그 리뷰 작성",
                "서울 강남구", "강남맛집,이탈리안");

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        CampaignDetail detail = campaignService.getDetail(1L);

        assertThat(detail.getTitle()).isEqualTo("테스트 캠페인");
        assertThat(detail.getSourceType()).isEqualTo("REVU");
        assertThat(detail.getSourceDisplayName()).isEqualTo("레뷰");
        assertThat(detail.getCategory()).isEqualTo("FOOD");
        assertThat(detail.getCategoryDisplayName()).isEqualTo("맛집");
        assertThat(detail.getStatus()).isEqualTo("RECRUITING");
        assertThat(detail.getStatusDisplayName()).isEqualTo("모집중");
        assertThat(detail.getDetailContent()).isEqualTo("상세 설명");
        assertThat(detail.getReward()).isEqualTo("2인 식사권");
        assertThat(detail.getMission()).isEqualTo("블로그 리뷰 작성");
        assertThat(detail.getAddress()).isEqualTo("서울 강남구");
        assertThat(detail.getKeywords()).containsExactly("강남맛집", "이탈리안");
    }

    @Test
    @DisplayName("존재하지 않는 ID → CampaignNotFoundException")
    void getDetail_notFound() {
        when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getDetail(999L))
                .isInstanceOf(CampaignNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("nullable 필드가 null인 데이터 매핑")
    void getDetail_nullableFields() {
        Campaign campaign = new Campaign(
                GANGNAM_SOURCE, "2001",
                "제목만 있는 캠페인", null, null,
                "https://gangnam.kr/2001",
                CampaignCategory.BEAUTY, CampaignStatus.CLOSED,
                null, null, null, null);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        CampaignDetail detail = campaignService.getDetail(1L);

        assertThat(detail.getDescription()).isNull();
        assertThat(detail.getDetailContent()).isNull();
        assertThat(detail.getReward()).isNull();
        assertThat(detail.getMission()).isNull();
        assertThat(detail.getAddress()).isNull();
        assertThat(detail.getRecruitCount()).isNull();
        assertThat(detail.getApplyStartDate()).isNull();
        assertThat(detail.getApplyEndDate()).isNull();
        assertThat(detail.getAnnouncementDate()).isNull();
    }

    @Test
    @DisplayName("keywords null → 빈 배열")
    void getDetail_keywordsNull_emptyList() {
        Campaign campaign = new Campaign(
                REVU_SOURCE, "1002",
                "키워드 없는 캠페인", null, null,
                "https://revu.net/1002",
                CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, null, null);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        CampaignDetail detail = campaignService.getDetail(1L);

        assertThat(detail.getKeywords()).isNotNull();
        assertThat(detail.getKeywords()).isEmpty();
    }
}
