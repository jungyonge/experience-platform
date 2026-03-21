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
class CampaignServiceSourceTest {

    @InjectMocks
    private CampaignService campaignService;

    @Mock
    private CampaignRepository campaignRepository;

    private static final CrawlingSource REVU_SOURCE =
            new CrawlingSource("REVU", "레뷰", "https://www.revu.net", null, null, "REVU", 1);
    private static final CrawlingSource GANGNAM_SOURCE =
            new CrawlingSource("GANGNAM", "강남맛집", "https://www.gangnam.kr", null, null, "GANGNAM", 2);

    @Test
    @DisplayName("sourceCodes 문자열로 필터 검색 - 활성 소스 코드 사용")
    void searchCampaigns_withSourceCodes() {
        Campaign campaign = new Campaign(
                REVU_SOURCE, "orig-1", "레뷰 캠페인", null, null,
                "https://revu.net/1", CampaignCategory.FOOD, CampaignStatus.RECRUITING,
                5, null, LocalDate.of(2026, 3, 31), null);
        Page<Campaign> page = new PageImpl<>(List.of(campaign));
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(page);

        CampaignListInfo result = campaignService.searchCampaigns(
                new CampaignSearchCommand(null, Set.of("REVU"), null, null, 0, 12, "latest"));

        assertThat(result.getCampaigns()).hasSize(1);
        assertThat(result.getCampaigns().get(0).getSourceType()).isEqualTo("REVU");

        verify(campaignRepository).searchByCondition(
                argThat(cond -> cond.getSourceCodes().contains("REVU")),
                any(Pageable.class));
    }

    @Test
    @DisplayName("여러 소스 코드로 필터 검색")
    void searchCampaigns_multipleSourceCodes() {
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(emptyPage);

        campaignService.searchCampaigns(
                new CampaignSearchCommand(null, Set.of("REVU", "GANGNAM"), null, null, 0, 12, "latest"));

        verify(campaignRepository).searchByCondition(
                argThat(cond ->
                        cond.getSourceCodes().contains("REVU") &&
                        cond.getSourceCodes().contains("GANGNAM")),
                any(Pageable.class));
    }

    @Test
    @DisplayName("소스 필터 없이 검색 - sourceCodes 빈 Set")
    void searchCampaigns_noSourceFilter() {
        Page<Campaign> emptyPage = new PageImpl<>(List.of());
        when(campaignRepository.searchByCondition(any(), any(Pageable.class))).thenReturn(emptyPage);

        campaignService.searchCampaigns(
                new CampaignSearchCommand(null, null, null, null, 0, 12, "latest"));

        verify(campaignRepository).searchByCondition(
                argThat(cond -> cond.getSourceCodes() != null && cond.getSourceCodes().isEmpty()),
                any(Pageable.class));
    }
}
