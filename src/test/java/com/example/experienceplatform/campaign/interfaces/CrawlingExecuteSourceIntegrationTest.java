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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CrawlingExecuteSourceIntegrationTest {

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
                        "email", "phase47exec@example.com",
                        "password", "Test12345!",
                        "passwordConfirm", "Test12345!",
                        "nickname", "Phase47실행"
                ))));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "phase47exec@example.com",
                                "password", "Test12345!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("POST /api/v1/admin/crawling/execute - 전체 실행 200, results 배열 포함")
    void executeAll_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.totalDurationMs").isNumber())
                .andExpect(jsonPath("$.executedAt").isString());
    }

    @Test
    @DisplayName("POST /api/v1/admin/crawling/execute/REVU - 소스별 실행 200")
    void executeBySource_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute/REVU")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("REVU"))
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    @DisplayName("POST /api/v1/admin/crawling/execute/NONEXIST - 존재하지 않는 소스 404")
    void executeBySource_notFound() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawling/execute/NONEXIST")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
