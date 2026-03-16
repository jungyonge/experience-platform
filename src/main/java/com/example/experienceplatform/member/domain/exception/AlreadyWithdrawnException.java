package com.example.experienceplatform.member.domain.exception;

public class AlreadyWithdrawnException extends BusinessException {

    public AlreadyWithdrawnException() {
        super("ALREADY_WITHDRAWN", "이미 탈퇴한 회원입니다.");
    }
}
