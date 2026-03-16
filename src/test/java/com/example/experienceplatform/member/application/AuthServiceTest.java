package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.*;
import com.example.experienceplatform.member.domain.exception.AccountDisabledException;
import com.example.experienceplatform.member.domain.exception.AuthenticationFailedException;
import com.example.experienceplatform.member.domain.exception.InvalidRefreshTokenException;
import com.example.experienceplatform.member.domain.exception.RefreshTokenExpiredException;
import com.example.experienceplatform.member.infrastructure.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Member createMember(Long id, String email, String nickname, MemberStatus status) throws Exception {
        Member member = new Member(
                new Email(email),
                new PasswordHash("encoded-password"),
                new Nickname(nickname));
        Field idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, id);
        if (status != MemberStatus.ACTIVE) {
            Field statusField = Member.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(member, status);
        }
        return member;
    }

    // ===== 로그인 테스트 =====

    @Test
    @DisplayName("로그인 성공 - 토큰 쌍 반환")
    void login_success() throws Exception {
        Member member = createMember(1L, "user@example.com", "testuser", MemberStatus.ACTIVE);
        when(memberRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L, "user@example.com")).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(1800L);
        when(jwtTokenProvider.getRefreshTokenExpiryMillis()).thenReturn(604800000L);
        when(refreshTokenRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        AuthTokenInfo result = authService.login(new LoginCommand("user@example.com", "password123"));

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getExpiresIn()).isEqualTo(1800L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 미존재")
    void login_emailNotFound() {
        when(memberRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("notfound@example.com", "password")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_passwordMismatch() throws Exception {
        Member member = createMember(1L, "user@example.com", "testuser", MemberStatus.ACTIVE);
        when(memberRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("user@example.com", "wrongpass")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 비활성 회원")
    void login_inactiveMember() throws Exception {
        Member member = createMember(1L, "user@example.com", "testuser", MemberStatus.INACTIVE);
        when(memberRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> authService.login(new LoginCommand("user@example.com", "password")))
                .isInstanceOf(AccountDisabledException.class);
    }

    // ===== 토큰 갱신 테스트 =====

    @Test
    @DisplayName("토큰 갱신 성공 - 새로운 토큰 쌍 반환 및 Refresh Token 교체")
    void refresh_success() throws Exception {
        Member member = createMember(1L, "user@example.com", "testuser", MemberStatus.ACTIVE);
        RefreshToken refreshToken = new RefreshToken(1L, "old-refresh-token",
                LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(1L, "user@example.com")).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(1800L);
        when(jwtTokenProvider.getRefreshTokenExpiryMillis()).thenReturn(604800000L);

        AuthTokenInfo result = authService.refresh(new RefreshTokenCommand("old-refresh-token"));

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(refreshToken.getToken()).isEqualTo("new-refresh-token"); // RTR 확인
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 존재하지 않는 Refresh Token")
    void refresh_invalidToken() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand("invalid-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 Refresh Token → 삭제 후 예외")
    void refresh_expiredToken() {
        RefreshToken expiredToken = new RefreshToken(1L, "expired-token",
                LocalDateTime.now().minusSeconds(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand("expired-token")))
                .isInstanceOf(RefreshTokenExpiredException.class);
        verify(refreshTokenRepository).deleteByMemberId(1L);
    }

    // ===== 로그아웃 테스트 =====

    @Test
    @DisplayName("로그아웃 - Refresh Token 삭제")
    void logout_deletesRefreshToken() {
        authService.logout(1L);

        verify(refreshTokenRepository).deleteByMemberId(1L);
    }
}
