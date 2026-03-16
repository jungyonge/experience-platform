package com.example.experienceplatform.member.interfaces;

public class LogoutResponse {

    private final String message;

    public LogoutResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
