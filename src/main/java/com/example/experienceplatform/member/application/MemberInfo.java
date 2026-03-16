package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.Member;
import com.example.experienceplatform.member.domain.MemberStatus;
import java.time.LocalDateTime;

public class MemberInfo {

    private final Long id;
    private final String email;
    private final String nickname;
    private final MemberStatus status;
    private final LocalDateTime createdAt;

    private MemberInfo(Long id, String email, String nickname, MemberStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static MemberInfo from(Member member) {
        return new MemberInfo(
                member.getId(),
                member.getEmail().getValue(),
                member.getNickname().getValue(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
