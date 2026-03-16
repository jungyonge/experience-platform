package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.member.application.MemberInfo;
import java.time.LocalDateTime;

public class SignupResponse {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String status;
    private final LocalDateTime createdAt;

    private SignupResponse(Long id, String email, String nickname, String status, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static SignupResponse from(MemberInfo info) {
        return new SignupResponse(
                info.getId(),
                info.getEmail(),
                info.getNickname(),
                info.getStatus().name(),
                info.getCreatedAt()
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

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
