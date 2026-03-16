package com.example.experienceplatform.member.domain.exception;

public class InvalidPasswordException extends BusinessException {

    public InvalidPasswordException() {
        super("INVALID_PASSWORD", "비밀번호는 8자 이상, 영문과 숫자/특수문자를 포함해야 합니다.");
    }
}
