package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.*;
import com.example.experienceplatform.member.domain.exception.DuplicateEmailException;
import com.example.experienceplatform.member.domain.exception.DuplicateNicknameException;
import com.example.experienceplatform.member.domain.exception.InvalidPasswordException;
import com.example.experienceplatform.member.domain.exception.PasswordMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterMemberCommand validCommand() {
        return new RegisterMemberCommand("test@example.com", "Test12345", "Test12345", "테스트유저");
    }

    @Test
    @DisplayName("정상 회원가입 성공")
    void registerMember_success() {
        RegisterMemberCommand command = validCommand();
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedHash");
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        MemberInfo result = memberService.registerMember(command);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("테스트유저");
        assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 PasswordMismatchException 발생")
    void registerMember_passwordMismatch() {
        RegisterMemberCommand command = new RegisterMemberCommand(
                "test@example.com", "Test12345", "Different1", "테스트유저");

        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    @DisplayName("비밀번호 정책 위반 시 InvalidPasswordException 발생")
    void registerMember_invalidPassword() {
        RegisterMemberCommand command = new RegisterMemberCommand(
                "test@example.com", "short", "short", "테스트유저");

        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    @DisplayName("이메일 중복 시 DuplicateEmailException 발생")
    void registerMember_duplicateEmail() {
        RegisterMemberCommand command = validCommand();
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedHash");
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("Unique index",
                        new RuntimeException("Unique index or primary key violation: MEMBERS(EMAIL NULLS FIRST) VALUES ('test'); SQL statement: insert")));

        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("닉네임 중복 시 DuplicateNicknameException 발생")
    void registerMember_duplicateNickname() {
        RegisterMemberCommand command = validCommand();
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedHash");
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("Unique index",
                        new RuntimeException("Unique index or primary key violation: MEMBERS(NICKNAME NULLS FIRST) VALUES ('test'); SQL statement: insert")));

        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    @Test
    @DisplayName("알 수 없는 DataIntegrityViolationException은 그대로 전파")
    void registerMember_unknownConstraintViolation() {
        RegisterMemberCommand command = validCommand();
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedHash");
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new DataIntegrityViolationException("Unknown constraint"));

        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
