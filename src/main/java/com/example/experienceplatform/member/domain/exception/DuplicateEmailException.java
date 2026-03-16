package com.example.experienceplatform.member.domain.exception;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
    }
}
