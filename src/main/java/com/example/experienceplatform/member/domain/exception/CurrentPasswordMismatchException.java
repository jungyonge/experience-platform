package com.example.experienceplatform.member.domain.exception;

public class CurrentPasswordMismatchException extends BusinessException {

    public CurrentPasswordMismatchException() {
        super("CURRENT_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다.");
    }
}
