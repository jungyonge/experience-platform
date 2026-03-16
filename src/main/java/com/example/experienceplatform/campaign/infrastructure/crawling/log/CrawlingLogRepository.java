package com.example.experienceplatform.campaign.infrastructure.crawling.log;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CrawlingLogRepository {

    CrawlingLog save(CrawlingLog crawlingLog);

    List<CrawlingLog> findRecent(Pageable pageable);

    List<CrawlingLog> findRecent(String sourceCode, Pageable pageable);

    Optional<CrawlingLog> findLatestBySourceCode(String sourceCode);
}
