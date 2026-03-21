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
class CampaignFilterOptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/campaigns/filters - 200 OK")
    void getFilters_ok() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTypes").isArray())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.statuses").isArray())
                .andExpect(jsonPath("$.sortOptions").isArray());
    }

    @Test
    @DisplayName("sourceTypes에 data.sql의 REVU, GANGNAM 포함")
    void getFilters_containsKnownSources() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTypes[*].code", hasItem("REVU")))
                .andExpect(jsonPath("$.sourceTypes[*].code", hasItem("GANGNAM")));
    }

    @Test
    @DisplayName("sourceTypes 항목은 code와 name 필드를 가짐")
    void getFilters_sourceTypeStructure() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTypes[0].code").isString())
                .andExpect(jsonPath("$.sourceTypes[0].name").isString());
    }

    @Test
    @DisplayName("인증 없이 접근 가능")
    void getFilters_noAuth() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/filters"))
                .andExpect(status().isOk());
    }
}
