package com.example.experienceplatform.member.domain.exception;

public class DuplicateNicknameException extends BusinessException {

    public DuplicateNicknameException() {
        super("DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");
    }
}
