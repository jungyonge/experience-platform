package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.MemberProfile;

import java.time.LocalDateTime;

public class MemberProfileResponse {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String status;
    private final String statusDisplayName;
    private final LocalDateTime createdAt;

    private MemberProfileResponse(Long id, String email, String nickname,
                                  String status, String statusDisplayName,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.statusDisplayName = statusDisplayName;
        this.createdAt = createdAt;
    }

    public static MemberProfileResponse from(MemberProfile profile) {
        return new MemberProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getNickname(),
                profile.getStatus(),
                profile.getStatusDisplayName(),
                profile.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getStatus() { return status; }
    public String getStatusDisplayName() { return statusDisplayName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
