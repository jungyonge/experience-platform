package com.example.experienceplatform.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    @Test
    @DisplayName("유효한 해시값으로 생성 성공")
    void createWithValidHash() {
        PasswordHash hash = new PasswordHash("$2a$10$encodedHashValue");
        assertThat(hash.getValue()).isEqualTo("$2a$10$encodedHashValue");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 또는 빈 문자열이면 예외 발생")
    void throwsWhenNullOrEmpty(String value) {
        assertThatThrownBy(() -> new PasswordHash(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("암호화된 비밀번호는 필수입니다.");
    }

    @Test
    @DisplayName("matches 메서드는 현재 false 반환")
    void matchesReturnsFalse() {
        PasswordHash hash = new PasswordHash("$2a$10$encodedHashValue");
        assertThat(hash.matches("rawPassword")).isFalse();
    }

    @Test
    @DisplayName("같은 값의 PasswordHash는 동등하다")
    void equalsByValue() {
        PasswordHash hash1 = new PasswordHash("$2a$10$encodedHashValue");
        PasswordHash hash2 = new PasswordHash("$2a$10$encodedHashValue");
        assertThat(hash1).isEqualTo(hash2);
    }
}
