package com.example.experienceplatform.member.infrastructure;

import com.example.experienceplatform.member.domain.RefreshToken;
import com.example.experienceplatform.member.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RefreshTokenJpaRepository implements RefreshTokenRepository {

    private final RefreshTokenSpringDataJpaRepository jpaRepository;

    public RefreshTokenJpaRepository(RefreshTokenSpringDataJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        jpaRepository.deleteByMemberId(memberId);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(refreshToken);
    }
}
