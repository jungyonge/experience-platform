package com.example.experienceplatform.campaign.application;

import com.example.experienceplatform.campaign.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @InjectMocks
    private CampaignService campaignService;

    @Mock
    private CampaignRepository campaignRepository;

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);
    private static final CrawlingSource GANGNAM_SOURCE =
            new CrawlingSource("GANGNAM", "강남맛집", "https://www.gangnam.kr", null, null, "GANGNAM", 2);

    private Campaign createCampaign(String title, CrawlingSource source, CampaignCategory category, CampaignStatus status) {
        return new Campaign(source, "orig-" + title, title, null, null,
                "https://example.com/" + title, category, status,
                5, null, LocalDate.of(2026, 3, 31), null);
    }

    @Test
    @DisplayName("기본 조건 목록 조회")
    void searchCampaigns_default() {
        Campaign c = createCampaign("테스트", REVU_SOURCE, CampaignCategory.FOOD, CampaignStatus.RECRUITING);
        Page<Campaign> page = new PageImpl<>(List.of(c));
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(page);

        CampaignListInfo result = campaignService.searchCampaigns(
                new CampaignSearchCommand(null, null, null, null, null, 0, 12, "latest"));

        assertThat(result.getCampaigns()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
        verify(campaignRepository).searchByCondition(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("키워드 검색 전달 확인")
    void searchCampaigns_withKeyword() {
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(emptyPage);

        campaignService.searchCampaigns(
                new CampaignSearchCommand("맛집", null, null, null, null, 0, 12, "latest"));

        verify(campaignRepository).searchByCondition(
                argThat(cond -> "맛집".equals(cond.getKeyword())),
                any(Pageable.class));
    }

    @Test
    @DisplayName("필터 조합 검색")
    void searchCampaigns_withFilters() {
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(emptyPage);

        campaignService.searchCampaigns(
                new CampaignSearchCommand(null, Set.of("REVU"), Set.of("FOOD"), "RECRUITING", null, 0, 12, "latest"));

        verify(campaignRepository).searchByCondition(
                argThat(cond ->
                        cond.getSourceCodes().contains("REVU")
                                && cond.getCategories().contains(CampaignCategory.FOOD)
                                && cond.getStatus() == CampaignStatus.RECRUITING),
                any(Pageable.class));
    }

    @Test
    @DisplayName("잘못된 enum 값 무시")
    void searchCampaigns_invalidEnumIgnored() {
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(emptyPage);

        campaignService.searchCampaigns(
                new CampaignSearchCommand(null, Set.of("INVALID_SOURCE"), Set.of("INVALID_CAT"), "INVALID_STATUS", null, 0, 12, "latest"));

        verify(campaignRepository).searchByCondition(
                argThat(cond ->
                        cond.getSourceCodes().contains("INVALID_SOURCE")
                                && cond.getCategories().isEmpty()
                                && cond.getStatus() == null),
                any(Pageable.class));
    }

    @Test
    @DisplayName("빈 keyword는 null로 변환")
    void searchCampaigns_emptyKeyword() {
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(emptyPage);

        campaignService.searchCampaigns(
                new CampaignSearchCommand("  ", null, null, null, null, 0, 12, "latest"));

        verify(campaignRepository).searchByCondition(
                argThat(cond -> cond.getKeyword() == null),
                any(Pageable.class));
    }

    @Test
    @DisplayName("CampaignSummary 변환 확인")
    void searchCampaigns_summaryMapping() {
        Campaign c = createCampaign("뷰티 체험", GANGNAM_SOURCE, CampaignCategory.BEAUTY, CampaignStatus.CLOSED);
        Page<Campaign> page = new PageImpl<>(List.of(c));
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(page);

        CampaignListInfo result = campaignService.searchCampaigns(
                new CampaignSearchCommand(null, null, null, null, null, 0, 12, "latest"));

        CampaignSummary summary = result.getCampaigns().get(0);
        assertThat(summary.getSourceType()).isEqualTo("GANGNAM");
        assertThat(summary.getSourceDisplayName()).isEqualTo("강남맛집");
        assertThat(summary.getCategory()).isEqualTo("BEAUTY");
        assertThat(summary.getCategoryDisplayName()).isEqualTo("뷰티");
        assertThat(summary.getStatus()).isEqualTo("CLOSED");
        assertThat(summary.getStatusDisplayName()).isEqualTo("모집마감");
    }
}
