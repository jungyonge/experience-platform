package com.example.experienceplatform.campaign.infrastructure.crawling.log;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CrawlingLogSpringDataJpaRepository extends JpaRepository<CrawlingLog, Long> {

    List<CrawlingLog> findByOrderByExecutedAtDesc(Pageable pageable);

    List<CrawlingLog> findBySourceCodeOrderByExecutedAtDesc(String sourceCode, Pageable pageable);

    Optional<CrawlingLog> findFirstBySourceCodeOrderByExecutedAtDesc(String sourceCode);
}
