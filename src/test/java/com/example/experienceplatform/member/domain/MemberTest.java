package com.example.experienceplatform.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("Member 생성 시 ACTIVE 상태")
    void createMember() {
        Member member = new Member(
                new Email("test@example.com"),
                new PasswordHash("$2a$10$encodedHash"),
                new Nickname("테스트유저")
        );

        assertThat(member.getEmail().getValue()).isEqualTo("test@example.com");
        assertThat(member.getNickname().getValue()).isEqualTo("테스트유저");
        assertThat(member.getPasswordHash().getValue()).isEqualTo("$2a$10$encodedHash");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        // createdAt/updatedAt은 @PrePersist에서 설정되므로 persistence 이전에는 null
    }
}
