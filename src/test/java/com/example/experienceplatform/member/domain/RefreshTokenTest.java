package com.example.experienceplatform.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    @DisplayName("만료되지 않은 토큰 - isExpired()가 false 반환")
    void isExpired_notExpired() {
        RefreshToken token = new RefreshToken(1L, "token-value",
                LocalDateTime.now().plusHours(1));

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 - isExpired()가 true 반환")
    void isExpired_expired() {
        RefreshToken token = new RefreshToken(1L, "token-value",
                LocalDateTime.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("rotate 호출 시 토큰과 만료일이 교체됨")
    void rotate_updatesTokenAndExpiry() {
        RefreshToken token = new RefreshToken(1L, "old-token",
                LocalDateTime.now().plusHours(1));

        LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);
        token.rotate("new-token", newExpiry);

        assertThat(token.getToken()).isEqualTo("new-token");
        assertThat(token.getExpiryDate()).isEqualTo(newExpiry);
    }

    @Test
    @DisplayName("RefreshToken 생성 시 필드가 올바르게 설정됨")
    void constructor_setsFieldsCorrectly() {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken(1L, "token-value", expiryDate);

        assertThat(token.getMemberId()).isEqualTo(1L);
        assertThat(token.getToken()).isEqualTo("token-value");
        assertThat(token.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(token.getCreatedAt()).isNotNull();
    }
}
