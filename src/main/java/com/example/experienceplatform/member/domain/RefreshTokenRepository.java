package com.example.experienceplatform.member.domain;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByMemberId(Long memberId);

    Optional<RefreshToken> findByToken(String token);

    void deleteByMemberId(Long memberId);

    RefreshToken save(RefreshToken refreshToken);
}
