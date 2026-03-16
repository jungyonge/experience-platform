package com.example.experienceplatform.member.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SIGNUP_URL = "/api/v1/members/signup";

    private String signupJson(String email, String password, String passwordConfirm, String nickname) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password,
                "passwordConfirm", passwordConfirm,
                "nickname", nickname
        ));
    }

    @Test
    @DisplayName("정상 회원가입 - 201 Created")
    void signup_success() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("new@example.com", "Test12345", "Test12345", "newuser")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.nickname").value("newuser"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("유효성 검증 실패 - 400 + VALIDATION_FAILED")
    void signup_validationFailed() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "email", "",
                "password", "short",
                "passwordConfirm", "short",
                "nickname", ""
        ));

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.path").value(SIGNUP_URL));
    }

    @Test
    @DisplayName("이메일 중복 - 409 + DUPLICATE_EMAIL")
    void signup_duplicateEmail() throws Exception {
        // 첫 번째 가입
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("dup@example.com", "Test12345", "Test12345", "user1")))
                .andExpect(status().isCreated());

        // 동일 이메일로 재가입
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("dup@example.com", "Test12345", "Test12345", "user2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("닉네임 중복 - 409 + DUPLICATE_NICKNAME")
    void signup_duplicateNickname() throws Exception {
        // 첫 번째 가입
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("nick1@example.com", "Test12345", "Test12345", "samenick")))
                .andExpect(status().isCreated());

        // 동일 닉네임으로 재가입
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("nick2@example.com", "Test12345", "Test12345", "samenick")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("비밀번호 불일치 - 400 + PASSWORD_MISMATCH")
    void signup_passwordMismatch() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("mismatch@example.com", "Test12345", "Different1", "mismatchuser")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("비밀번호 정책 위반 - 400 + INVALID_PASSWORD")
    void signup_invalidPassword() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("policy@example.com", "onlyletters", "onlyletters", "policyuser")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
