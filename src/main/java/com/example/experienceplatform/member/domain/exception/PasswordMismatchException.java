package com.example.experienceplatform.member.domain.exception;

public class PasswordMismatchException extends BusinessException {

    public PasswordMismatchException() {
        super("PASSWORD_MISMATCH", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }
}
