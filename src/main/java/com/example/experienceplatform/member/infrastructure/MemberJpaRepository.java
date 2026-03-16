package com.example.experienceplatform.member.infrastructure;

import com.example.experienceplatform.member.domain.Member;
import com.example.experienceplatform.member.domain.MemberRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberJpaRepository extends JpaRepository<Member, Long>, MemberRepository {
}
