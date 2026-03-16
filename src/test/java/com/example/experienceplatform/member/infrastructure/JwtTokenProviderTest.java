package com.example.experienceplatform.member.infrastructure;

import com.example.experienceplatform.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("my-super-secret-key-for-local-development-only-32bytes");
        properties.setAccessTokenExpiry(1800000); // 30분
        properties.setRefreshTokenExpiry(604800000); // 7일
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("Access Token 생성 후 memberId 추출")
    void createAccessToken_andExtractMemberId() {
        String token = jwtTokenProvider.createAccessToken(1L, "user@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.getMemberIdFromToken(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("유효한 Access Token 검증 성공")
    void validateToken_validToken() {
        String token = jwtTokenProvider.createAccessToken(1L, "user@example.com");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 false 반환")
    void validateToken_expiredToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("my-super-secret-key-for-local-development-only-32bytes");
        properties.setAccessTokenExpiry(0); // 즉시 만료
        properties.setRefreshTokenExpiry(604800000);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(properties);

        String token = shortLivedProvider.createAccessToken(1L, "user@example.com");

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
        assertThat(shortLivedProvider.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰 검증 시 false 반환")
    void validateToken_tamperedToken() {
        String token = jwtTokenProvider.createAccessToken(1L, "user@example.com");
        String tampered = token + "tampered";

        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("다른 시크릿 키로 서명된 토큰 검증 시 false 반환")
    void validateToken_differentSecret() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("another-secret-key-for-test-purposes-32bytes!!");
        otherProperties.setAccessTokenExpiry(1800000);
        otherProperties.setRefreshTokenExpiry(604800000);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);

        String token = otherProvider.createAccessToken(1L, "user@example.com");

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 생성 - UUID 형식 확인")
    void createRefreshToken_uuidFormat() {
        String refreshToken = jwtTokenProvider.createRefreshToken();

        assertThat(refreshToken).isNotBlank();
        // UUID 형식 검증
        UUID.fromString(refreshToken);
    }

    @Test
    @DisplayName("만료되지 않은 토큰 - isTokenExpired()가 false 반환")
    void isTokenExpired_validToken() {
        String token = jwtTokenProvider.createAccessToken(1L, "user@example.com");

        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("getAccessTokenExpirySeconds - 초 단위 반환")
    void getAccessTokenExpirySeconds() {
        assertThat(jwtTokenProvider.getAccessTokenExpirySeconds()).isEqualTo(1800);
    }
}
