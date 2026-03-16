package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.CrawlingSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CrawlingSourceSpringDataJpaRepository extends JpaRepository<CrawlingSource, Long> {

    Optional<CrawlingSource> findByCode(String code);

    List<CrawlingSource> findByActiveTrueOrderByDisplayOrderAsc();

    List<CrawlingSource> findAllByOrderByDisplayOrderAsc();

    boolean existsByCode(String code);
}
