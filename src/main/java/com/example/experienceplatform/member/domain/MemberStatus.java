package com.example.experienceplatform.member.domain;

public enum MemberStatus {

    ACTIVE("활성"),
    INACTIVE("비활성"),
    WITHDRAWN("탈퇴");

    private final String displayName;

    MemberStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
