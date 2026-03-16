package com.example.experienceplatform.member.domain.exception;

public class MemberNotFoundException extends BusinessException {

    public MemberNotFoundException(Long memberId) {
        super("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다. id=" + memberId);
    }
}
