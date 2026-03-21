package com.example.experienceplatform.campaign.interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("기본 캠페인 목록 조회 - 200 OK")
    void searchCampaigns_default() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaigns").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    @DisplayName("키워드 검색 - 200 OK")
    void searchCampaigns_keyword() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns").param("keyword", "체험단"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaigns").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    @Test
    @DisplayName("소스/카테고리/상태 필터 조합 - 200 OK")
    void searchCampaigns_filters() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns")
                        .param("sourceTypes", "REVU,GANGNAM")
                        .param("categories", "FOOD")
                        .param("status", "RECRUITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaigns").isArray());
    }

    @Test
    @DisplayName("잘못된 sort 값 - 400")
    void searchCampaigns_invalidSort() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns").param("sort", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_VALUE"));
    }

    @Test
    @DisplayName("size 초과 - 400")
    void searchCampaigns_sizeExceeded() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
    }

    @Test
    @DisplayName("인증 없이 접근 가능")
    void searchCampaigns_noAuth() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("필터 옵션 조회 - 200 OK")
    void getFilters() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTypes").isArray())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.statuses").isArray())
                .andExpect(jsonPath("$.sortOptions").isArray())
                .andExpect(jsonPath("$.sourceTypes[0].code").value("REVU"))
                .andExpect(jsonPath("$.statuses").value(org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("필터 옵션 - 인증 없이 접근 가능")
    void getFilters_noAuth() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/filters"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("시드 데이터 기반 - 총 건수 확인")
    void searchCampaigns_seedDataCount() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(50)));
    }
}
