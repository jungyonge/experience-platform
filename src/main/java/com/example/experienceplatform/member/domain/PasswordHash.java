package com.example.experienceplatform.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class PasswordHash {

    @Column(name = "password", nullable = false)
    private String value;

    protected PasswordHash() {
    }

    public PasswordHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("암호화된 비밀번호는 필수입니다.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String rawPassword) {
        // 향후 구현 예정
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordHash that = (PasswordHash) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
