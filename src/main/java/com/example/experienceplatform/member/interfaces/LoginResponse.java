package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.AuthTokenInfo;

public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;

    private LoginResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public static LoginResponse from(AuthTokenInfo info) {
        return new LoginResponse(info.getAccessToken(), info.getRefreshToken(), info.getExpiresIn());
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
