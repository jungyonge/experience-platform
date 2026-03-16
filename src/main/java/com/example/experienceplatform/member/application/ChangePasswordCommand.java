package com.example.experienceplatform.member.application;

public class ChangePasswordCommand {

    private final Long memberId;
    private final String currentPassword;
    private final String newPassword;
    private final String newPasswordConfirm;

    public ChangePasswordCommand(Long memberId, String currentPassword,
                                 String newPassword, String newPasswordConfirm) {
        this.memberId = memberId;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.newPasswordConfirm = newPasswordConfirm;
    }

    public Long getMemberId() { return memberId; }
    public String getCurrentPassword() { return currentPassword; }
    public String getNewPassword() { return newPassword; }
    public String getNewPasswordConfirm() { return newPasswordConfirm; }
}
