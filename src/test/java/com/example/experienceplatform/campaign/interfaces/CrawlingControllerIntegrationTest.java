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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CrawlingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 회원 생성 및 로그인
        mockMvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "crawltest@example.com",
                        "password", "Test12345!",
                        "passwordConfirm", "Test12345!",
                        "nickname", "크롤링테스트"
                ))));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "crawltest@example.com",
                                "password", "Test12345!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("전체 크롤링 실행 - 인증 + 200")
    void executeAll_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.totalDurationMs").isNumber())
                .andExpect(jsonPath("$.executedAt").isString());
    }

    @Test
    @DisplayName("소스별 크롤링 실행 - 200")
    void executeBySource_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute/REVU")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("REVU"))
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    @DisplayName("잘못된 sourceCode - 404")
    void executeBySource_invalid() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute/INVALID")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("로그 조회 - 200")
    void getLogs_success() throws Exception {
        // 먼저 크롤링 실행하여 로그 생성
        mockMvc.perform(post("/api/v1/admin/crawling/execute")
                .header("Authorization", "Bearer " + accessToken));

        mockMvc.perform(get("/api/v1/admin/crawling/logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray());
    }

    @Test
    @DisplayName("미인증 - 401")
    void executeAll_noAuth() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute"))
                .andExpect(status().isUnauthorized());
    }
}
