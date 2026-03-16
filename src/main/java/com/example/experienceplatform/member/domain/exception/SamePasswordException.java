package com.example.experienceplatform.member.domain.exception;

public class SamePasswordException extends BusinessException {

    public SamePasswordException() {
        super("SAME_PASSWORD", "현재 비밀번호와 새 비밀번호가 동일합니다.");
    }
}
