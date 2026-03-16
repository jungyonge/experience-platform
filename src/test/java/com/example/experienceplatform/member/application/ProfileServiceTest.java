package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.*;
import com.example.experienceplatform.member.domain.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @InjectMocks
    private ProfileService profileService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Member createMember() {
        return new Member(
                new Email("test@example.com"),
                new PasswordHash("$2a$10$encodedHash"),
                new Nickname("테스트유저")
        );
    }

    // === 프로필 조회 ===

    @Test
    @DisplayName("프로필 조회 - 정상")
    void getProfile_success() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberProfile profile = profileService.getProfile(1L);

        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(profile.getNickname()).isEqualTo("테스트유저");
        assertThat(profile.getStatus()).isEqualTo("ACTIVE");
        assertThat(profile.getStatusDisplayName()).isEqualTo("활성");
    }

    @Test
    @DisplayName("프로필 조회 - 존재하지 않는 ID")
    void getProfile_notFound() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(999L))
                .isInstanceOf(MemberNotFoundException.class);
    }

    // === 닉네임 수정 ===

    @Test
    @DisplayName("닉네임 수정 - 정상 변경")
    void changeNickname_success() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(any())).thenReturn(false);
        when(memberRepository.saveAndFlush(any())).thenReturn(member);

        MemberProfile result = profileService.changeNickname(
                new ChangeNicknameCommand(1L, "새닉네임"));

        assertThat(result.getNickname()).isEqualTo("새닉네임");
        verify(memberRepository).saveAndFlush(member);
    }

    @Test
    @DisplayName("닉네임 수정 - 동일 닉네임 → 변경 없이 성공")
    void changeNickname_sameNickname() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberProfile result = profileService.changeNickname(
                new ChangeNicknameCommand(1L, "테스트유저"));

        assertThat(result.getNickname()).isEqualTo("테스트유저");
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("닉네임 수정 - 중복 → DuplicateNicknameException")
    void changeNickname_duplicate() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(any())).thenReturn(true);

        assertThatThrownBy(() -> profileService.changeNickname(
                new ChangeNicknameCommand(1L, "중복닉네임")))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    @Test
    @DisplayName("닉네임 수정 - DB Unique 위반 fallback")
    void changeNickname_dbUnique() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(any())).thenReturn(false);
        when(memberRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> profileService.changeNickname(
                new ChangeNicknameCommand(1L, "새닉네임")))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    // === 비밀번호 변경 ===

    @Test
    @DisplayName("비밀번호 변경 - 정상")
    void changePassword_success() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("oldPassword1!", "$2a$10$encodedHash")).thenReturn(true);
        when(passwordEncoder.matches("newPassword1!", "$2a$10$encodedHash")).thenReturn(false);
        when(passwordEncoder.encode("newPassword1!")).thenReturn("$2a$10$newHash");

        profileService.changePassword(
                new ChangePasswordCommand(1L, "oldPassword1!", "newPassword1!", "newPassword1!"));

        verify(memberRepository).save(member);
        assertThat(member.getPasswordHash().getValue()).isEqualTo("$2a$10$newHash");
    }

    @Test
    @DisplayName("비밀번호 변경 - 새 비밀번호 확인 불일치")
    void changePassword_mismatch() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> profileService.changePassword(
                new ChangePasswordCommand(1L, "old1!", "newPass1!", "different1!")))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 - 정책 위반")
    void changePassword_invalidPolicy() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> profileService.changePassword(
                new ChangePasswordCommand(1L, "old1!", "weak", "weak")))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치")
    void changePassword_currentMismatch() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrongPassword1!", "$2a$10$encodedHash")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(
                new ChangePasswordCommand(1L, "wrongPassword1!", "newPassword1!", "newPassword1!")))
                .isInstanceOf(CurrentPasswordMismatchException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재/새 비밀번호 동일")
    void changePassword_samePassword() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("samePassword1!", "$2a$10$encodedHash")).thenReturn(true);

        assertThatThrownBy(() -> profileService.changePassword(
                new ChangePasswordCommand(1L, "samePassword1!", "samePassword1!", "samePassword1!")))
                .isInstanceOf(SamePasswordException.class);
    }

    // === 회원 탈퇴 ===

    @Test
    @DisplayName("회원 탈퇴 - 정상")
    void withdraw_success() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password1!", "$2a$10$encodedHash")).thenReturn(true);

        profileService.withdraw(new WithdrawMemberCommand(1L, "password1!"));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        verify(memberRepository).save(member);
        verify(refreshTokenRepository).deleteByMemberId(1L);
    }

    @Test
    @DisplayName("회원 탈퇴 - 현재 비밀번호 불일치")
    void withdraw_passwordMismatch() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong!", "$2a$10$encodedHash")).thenReturn(false);

        assertThatThrownBy(() -> profileService.withdraw(
                new WithdrawMemberCommand(1L, "wrong!")))
                .isInstanceOf(CurrentPasswordMismatchException.class);
    }

    @Test
    @DisplayName("회원 탈퇴 - 이미 탈퇴한 회원")
    void withdraw_alreadyWithdrawn() {
        Member member = createMember();
        member.withdraw(); // 먼저 탈퇴
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password1!", "$2a$10$encodedHash")).thenReturn(true);

        assertThatThrownBy(() -> profileService.withdraw(
                new WithdrawMemberCommand(1L, "password1!")))
                .isInstanceOf(AlreadyWithdrawnException.class);
    }
}
