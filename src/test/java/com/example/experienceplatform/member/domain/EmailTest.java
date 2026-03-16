package com.example.experienceplatform.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    @DisplayName("유효한 이메일로 생성 성공")
    void createWithValidEmail() {
        Email email = new Email("test@example.com");
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 또는 빈 문자열이면 예외 발생")
    void throwsWhenNullOrEmpty(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "no-at-sign.com", "@no-local.com", "missing@.com"})
    @DisplayName("이메일 형식이 아니면 예외 발생")
    void throwsWhenInvalidFormat(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("같은 값의 Email은 동등하다")
    void equalsByValue() {
        Email email1 = new Email("test@example.com");
        Email email2 = new Email("test@example.com");
        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }
}
