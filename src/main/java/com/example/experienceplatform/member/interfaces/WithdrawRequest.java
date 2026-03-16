package com.example.experienceplatform.member.interfaces;

import jakarta.validation.constraints.NotBlank;

public class WithdrawRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    protected WithdrawRequest() {
    }

    public WithdrawRequest(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getCurrentPassword() { return currentPassword; }
}
