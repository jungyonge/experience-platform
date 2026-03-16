package com.example.experienceplatform.member.domain;

import com.example.experienceplatform.member.domain.exception.AlreadyWithdrawnException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberDomainExtensionTest {

    private Member createMember() {
        return new Member(
                new Email("test@example.com"),
                new PasswordHash("$2a$10$encodedHash"),
                new Nickname("테스트유저")
        );
    }

    @Test
    @DisplayName("changeNickname - 닉네임 변경")
    void changeNickname() {
        Member member = createMember();
        Nickname newNickname = new Nickname("새닉네임");

        member.changeNickname(newNickname);

        assertThat(member.getNickname().getValue()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("changePassword - 비밀번호 해시 변경")
    void changePassword() {
        Member member = createMember();
        PasswordHash newHash = new PasswordHash("$2a$10$newEncodedHash");

        member.changePassword(newHash);

        assertThat(member.getPasswordHash().getValue()).isEqualTo("$2a$10$newEncodedHash");
    }

    @Test
    @DisplayName("withdraw - 상태 WITHDRAWN 으로 변경")
    void withdraw() {
        Member member = createMember();

        member.withdraw();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("withdraw 재호출 - AlreadyWithdrawnException")
    void withdraw_alreadyWithdrawn() {
        Member member = createMember();
        member.withdraw();

        assertThatThrownBy(member::withdraw)
                .isInstanceOf(AlreadyWithdrawnException.class);
    }

    @Test
    @DisplayName("MemberStatus displayName 매핑")
    void memberStatus_displayName() {
        assertThat(MemberStatus.ACTIVE.getDisplayName()).isEqualTo("활성");
        assertThat(MemberStatus.INACTIVE.getDisplayName()).isEqualTo("비활성");
        assertThat(MemberStatus.WITHDRAWN.getDisplayName()).isEqualTo("탈퇴");
    }
}
