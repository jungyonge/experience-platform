package com.example.experienceplatform.member.application;

public class ChangeNicknameCommand {

    private final Long memberId;
    private final String newNickname;

    public ChangeNicknameCommand(Long memberId, String newNickname) {
        this.memberId = memberId;
        this.newNickname = newNickname;
    }

    public Long getMemberId() { return memberId; }
    public String getNewNickname() { return newNickname; }
}
