package com.example.experienceplatform.campaign.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCrawlingSourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "phase47test@example.com",
                        "password", "Test12345!",
                        "passwordConfirm", "Test12345!",
                        "nickname", "Phase47테스트"
                ))));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "phase47test@example.com",
                                "password", "Test12345!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("GET 소스 목록 조회 - 200, sources와 availableCrawlerTypes 포함")
    void getSources_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/crawling/sources")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.availableCrawlerTypes").isArray());
    }

    @Test
    @DisplayName("POST 소스 생성 - 201")
    void createSource_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PH47_NEW",
                                "name", "Phase47신규소스",
                                "baseUrl", "https://phase47.com",
                                "crawlerType", "REVU",
                                "displayOrder", 99
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PH47_NEW"))
                .andExpect(jsonPath("$.name").value("Phase47신규소스"));
    }

    @Test
    @DisplayName("POST 중복 코드 소스 생성 - 409")
    void createSource_duplicate() throws Exception {
        // data.sql에 REVU가 이미 존재
        mockMvc.perform(post("/api/v1/admin/crawling/sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "REVU",
                                "name", "중복레뷰",
                                "baseUrl", "https://revu.net",
                                "crawlerType", "REVU",
                                "displayOrder", 1
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SOURCE_CODE"));
    }

    @Test
    @DisplayName("PUT 소스 수정 - 200")
    void updateSource_success() throws Exception {
        // 먼저 소스 생성
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/crawling/sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PH47_UPD",
                                "name", "수정대상",
                                "baseUrl", "https://update-test.com",
                                "crawlerType", "REVU",
                                "displayOrder", 50
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Long sourceId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/crawling/sources/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "수정완료",
                                "baseUrl", "https://updated.com",
                                "crawlerType", "REVU",
                                "displayOrder", 51
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정완료"))
                .andExpect(jsonPath("$.baseUrl").value("https://updated.com"));
    }

    @Test
    @DisplayName("PATCH 활성/비활성 토글 - 200")
    void toggleActive_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/crawling/sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PH47_TGL",
                                "name", "토글대상",
                                "baseUrl", "https://toggle.com",
                                "crawlerType", "REVU",
                                "displayOrder", 55
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Long sourceId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        // 초기 active=true, 토글 후 false
        mockMvc.perform(patch("/api/v1/admin/crawling/sources/" + sourceId + "/toggle-active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("POST 테스트 크롤 - 200")
    void testCrawl_success() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/crawling/sources")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "PH47_TST",
                                "name", "테스트크롤",
                                "baseUrl", "https://testcrawl.com",
                                "crawlerType", "REVU",
                                "displayOrder", 60
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Long sourceId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/crawling/sources/" + sourceId + "/test")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCode").value("PH47_TST"))
                .andExpect(jsonPath("$.crawlerType").value("REVU"));
    }

    @Test
    @DisplayName("미인증 요청 - 401")
    void getSources_noAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/crawling/sources"))
                .andExpect(status().isUnauthorized());
    }
}
