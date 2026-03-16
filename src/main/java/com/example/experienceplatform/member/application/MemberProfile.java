package com.example.experienceplatform.member.application;

import com.example.experienceplatform.member.domain.Member;

import java.time.LocalDateTime;

public class MemberProfile {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String status;
    private final String statusDisplayName;
    private final LocalDateTime createdAt;

    private MemberProfile(Long id, String email, String nickname,
                          String status, String statusDisplayName,
                          LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.statusDisplayName = statusDisplayName;
        this.createdAt = createdAt;
    }

    public static MemberProfile from(Member member) {
        return new MemberProfile(
                member.getId(),
                member.getEmail().getValue(),
                member.getNickname().getValue(),
                member.getStatus().name(),
                member.getStatus().getDisplayName(),
                member.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getStatus() { return status; }
    public String getStatusDisplayName() { return statusDisplayName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
