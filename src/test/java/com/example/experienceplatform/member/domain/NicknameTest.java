package com.example.experienceplatform.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NicknameTest {

    @Test
    @DisplayName("유효한 닉네임으로 생성 성공 - 한글")
    void createWithKorean() {
        Nickname nickname = new Nickname("테스트유저");
        assertThat(nickname.getValue()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("유효한 닉네임으로 생성 성공 - 영문+숫자")
    void createWithAlphanumeric() {
        Nickname nickname = new Nickname("user123");
        assertThat(nickname.getValue()).isEqualTo("user123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 또는 빈 문자열이면 예외 발생")
    void throwsWhenNullOrEmpty(String value) {
        assertThatThrownBy(() -> new Nickname(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "일", "abcdefghijklmnopqrstu", "nick name", "nick!@#"})
    @DisplayName("정책 위반 닉네임이면 예외 발생 (1자, 21자 초과, 공백, 특수문자)")
    void throwsWhenInvalidFormat(String value) {
        assertThatThrownBy(() -> new Nickname(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 2~20자의 한글, 영문, 숫자만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("같은 값의 Nickname은 동등하다")
    void equalsByValue() {
        Nickname n1 = new Nickname("테스트");
        Nickname n2 = new Nickname("테스트");
        assertThat(n1).isEqualTo(n2);
        assertThat(n1.hashCode()).isEqualTo(n2.hashCode());
    }
}
