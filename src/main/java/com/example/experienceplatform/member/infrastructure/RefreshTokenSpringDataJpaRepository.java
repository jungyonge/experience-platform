package com.example.experienceplatform.member.infrastructure;

import com.example.experienceplatform.member.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface RefreshTokenSpringDataJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMemberId(Long memberId);

    Optional<RefreshToken> findByToken(String token);

    void deleteByMemberId(Long memberId);
}
