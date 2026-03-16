package com.example.experienceplatform.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class Nickname {

    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[가-힣a-zA-Z0-9]{2,20}$");

    @Column(name = "nickname", nullable = false, unique = true)
    private String value;

    protected Nickname() {
    }

    public Nickname(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (!NICKNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("닉네임은 2~20자의 한글, 영문, 숫자만 사용할 수 있습니다.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nickname nickname = (Nickname) o;
        return Objects.equals(value, nickname.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
