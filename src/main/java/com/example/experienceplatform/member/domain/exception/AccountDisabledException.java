package com.example.experienceplatform.member.domain.exception;

public class AccountDisabledException extends BusinessException {

    public AccountDisabledException() {
        super("ACCOUNT_DISABLED", "비활성화되었거나 탈퇴한 계정입니다.");
    }
}
