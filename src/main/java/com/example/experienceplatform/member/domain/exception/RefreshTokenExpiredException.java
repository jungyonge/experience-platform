package com.example.experienceplatform.member.domain.exception;

public class RefreshTokenExpiredException extends BusinessException {

    public RefreshTokenExpiredException() {
        super("REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었거나 유효하지 않습니다.");
    }
}
