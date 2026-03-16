package com.example.experienceplatform.member.domain;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Member saveAndFlush(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);

    boolean existsByNickname(Nickname nickname);
}
