package com.example.experienceplatform.member.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SIGNUP_URL = "/api/v1/members/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PASSWORD = "Test12345!";

    private String createUserAndLogin(String suffix) throws Exception {
        String email = "ptest" + suffix + "@example.com";
        String nickname = "pt" + suffix;

        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", PASSWORD,
                        "passwordConfirm", PASSWORD,
                        "nickname", nickname
                ))));

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String uniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // === 프로필 조회 ===

    @Test
    @DisplayName("프로필 조회 - 인증 + 정상 → 200")
    void getProfile_success() throws Exception {
        String token = createUserAndLogin(uniqueId());

        mockMvc.perform(get("/api/v1/members/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.nickname").isString())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusDisplayName").value("활성"))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    @DisplayName("프로필 조회 - 미인증 → 401")
    void getProfile_noAuth() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    // === 닉네임 수정 ===

    @Test
    @DisplayName("닉네임 수정 - 정상 → 200")
    void changeNickname_success() throws Exception {
        String token = createUserAndLogin(uniqueId());
        String newNick = "새닉" + uniqueId().substring(0, 4);

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", newNick))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(newNick));
    }

    @Test
    @DisplayName("닉네임 수정 - 형식 오류 → 400")
    void changeNickname_invalid() throws Exception {
        String token = createUserAndLogin(uniqueId());

        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "a"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("닉네임 수정 - 미인증 → 401")
    void changeNickname_noAuth() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "새닉네임"))))
                .andExpect(status().isUnauthorized());
    }

    // === 비밀번호 변경 ===

    @Test
    @DisplayName("비밀번호 변경 - 정상 → 200")
    void changePassword_success() throws Exception {
        String token = createUserAndLogin(uniqueId());

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", "NewPass123!",
                                "newPasswordConfirm", "NewPass123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치 → 400")
    void changePassword_currentMismatch() throws Exception {
        String token = createUserAndLogin(uniqueId());

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongPass1!",
                                "newPassword", "NewPass123!",
                                "newPasswordConfirm", "NewPass123!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("비밀번호 변경 - 미인증 → 401")
    void changePassword_noAuth() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", "NewPass123!",
                                "newPasswordConfirm", "NewPass123!"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // === 회원 탈퇴 ===

    @Test
    @DisplayName("회원 탈퇴 - 정상 → 200")
    void withdraw_success() throws Exception {
        String token = createUserAndLogin(uniqueId());

        mockMvc.perform(delete("/api/v1/members/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    @DisplayName("회원 탈퇴 - 현재 비밀번호 불일치 → 400")
    void withdraw_passwordMismatch() throws Exception {
        String token = createUserAndLogin(uniqueId());

        mockMvc.perform(delete("/api/v1/members/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongPass1!"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("회원 탈퇴 - 미인증 → 401")
    void withdraw_noAuth() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴 후 재로그인 → 403 ACCOUNT_DISABLED")
    void withdraw_thenLogin() throws Exception {
        String uid = uniqueId();
        String email = "ptest" + uid + "@example.com";
        String nickname = "pt" + uid;

        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", PASSWORD,
                        "passwordConfirm", PASSWORD,
                        "nickname", nickname
                ))));

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        // 탈퇴
        mockMvc.perform(delete("/api/v1/members/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD
                        ))))
                .andExpect(status().isOk());

        // 재로그인 시도 → 403
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }
}
