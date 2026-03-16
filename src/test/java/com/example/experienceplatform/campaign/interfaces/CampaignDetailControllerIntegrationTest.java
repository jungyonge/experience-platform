package com.example.experienceplatform.campaign.interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignDetailControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("정상 상세 조회 - 200 OK")
    void getDetail_success() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.sourceType").isString())
                .andExpect(jsonPath("$.sourceDisplayName").isString())
                .andExpect(jsonPath("$.category").isString())
                .andExpect(jsonPath("$.categoryDisplayName").isString())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.statusDisplayName").isString())
                .andExpect(jsonPath("$.originalUrl").isString())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
    }

    @Test
    @DisplayName("상세 조회 - 상세 필드 포함 확인 (ID=1 시드 데이터)")
    void getDetail_withDetailFields() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detailContent").isNotEmpty())
                .andExpect(jsonPath("$.reward").isNotEmpty())
                .andExpect(jsonPath("$.mission").isNotEmpty())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.applyStartDate").isString())
                .andExpect(jsonPath("$.applyEndDate").isString())
                .andExpect(jsonPath("$.announcementDate").isString());
    }

    @Test
    @DisplayName("존재하지 않는 ID → 404 CAMPAIGN_NOT_FOUND")
    void getDetail_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_NOT_FOUND"));
    }

    @Test
    @DisplayName("문자열 ID → 400 INVALID_PARAMETER")
    void getDetail_invalidId() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("nullable 필드 null 직렬화 확인")
    void getDetail_nullableFieldsSerialization() throws Exception {
        // ID=18 (REVU 1018) - detailContent, address가 null인 시드 데이터
        mockMvc.perform(get("/api/v1/campaigns/18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detailContent").doesNotExist())
                .andExpect(jsonPath("$.address").doesNotExist());
    }

    @Test
    @DisplayName("keywords 빈 경우 빈 배열 응답")
    void getDetail_emptyKeywords() throws Exception {
        // ID=18 (REVU 1018) - keywords가 null인 시드 데이터
        mockMvc.perform(get("/api/v1/campaigns/18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords", hasSize(0)));
    }

    @Test
    @DisplayName("인증 없이 상세 조회 접근 가능")
    void getDetail_noAuth() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/1"))
                .andExpect(status().isOk());
    }
}
