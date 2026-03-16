package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.config.JwtProperties;
import com.example.experienceplatform.member.infrastructure.JwtTokenProvider;
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
class MemberControllerMeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    private static final String SIGNUP_URL = "/api/v1/members/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/v1/members/me";

    private static final String TEST_EMAIL = "metest@example.com";
    private static final String TEST_PASSWORD = "Test12345";
    private static final String TEST_NICKNAME = "meuser";

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", TEST_EMAIL,
                        "password", TEST_PASSWORD,
                        "passwordConfirm", TEST_PASSWORD,
                        "nickname", TEST_NICKNAME
                ))));
    }

    @Test
    @DisplayName("유효한 토큰으로 내 정보 조회 - 200 OK")
    void me_withValidToken() throws Exception {
        // 로그인
        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TEST_EMAIL,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        // /me 조회
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.nickname").value(TEST_NICKNAME))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("토큰 없이 내 정보 조회 - 401 UNAUTHORIZED")
    void me_withoutToken() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("만료된 토큰으로 내 정보 조회 - 401 TOKEN_EXPIRED")
    void me_withExpiredToken() throws Exception {
        // 즉시 만료되는 토큰 생성
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(jwtProperties.getSecret());
        expiredProps.setAccessTokenExpiry(0);
        expiredProps.setRefreshTokenExpiry(604800000);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

        String expiredToken = expiredProvider.createAccessToken(1L, TEST_EMAIL);

        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 내 정보 조회 - 401 INVALID_TOKEN")
    void me_withInvalidToken() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }
}
