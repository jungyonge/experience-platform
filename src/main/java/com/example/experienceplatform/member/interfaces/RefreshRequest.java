package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.RefreshTokenCommand;
import jakarta.validation.constraints.NotBlank;

public class RefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;

    public RefreshRequest() {
    }

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken);
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
