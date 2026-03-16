package com.example.experienceplatform.member.application;

public class WithdrawMemberCommand {

    private final Long memberId;
    private final String currentPassword;

    public WithdrawMemberCommand(Long memberId, String currentPassword) {
        this.memberId = memberId;
        this.currentPassword = currentPassword;
    }

    public Long getMemberId() { return memberId; }
    public String getCurrentPassword() { return currentPassword; }
}
