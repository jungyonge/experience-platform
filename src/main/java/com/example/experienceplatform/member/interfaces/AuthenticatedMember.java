package com.example.experienceplatform.member.interfaces;

import java.security.Principal;

public class AuthenticatedMember implements Principal {

    private final Long memberId;

    public AuthenticatedMember(Long memberId) {
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
