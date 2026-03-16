package com.example.experienceplatform.member.domain.exception;

public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException() {
        super("AUTHENTICATION_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
