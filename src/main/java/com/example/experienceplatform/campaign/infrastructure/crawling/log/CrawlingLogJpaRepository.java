package com.example.experienceplatform.campaign.infrastructure.crawling.log;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CrawlingLogJpaRepository implements CrawlingLogRepository {

    private final CrawlingLogSpringDataJpaRepository springDataRepository;

    public CrawlingLogJpaRepository(CrawlingLogSpringDataJpaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CrawlingLog save(CrawlingLog crawlingLog) {
        return springDataRepository.save(crawlingLog);
    }

    @Override
    public List<CrawlingLog> findRecent(Pageable pageable) {
        return springDataRepository.findByOrderByExecutedAtDesc(pageable);
    }

    @Override
    public List<CrawlingLog> findRecent(String sourceCode, Pageable pageable) {
        return springDataRepository.findBySourceCodeOrderByExecutedAtDesc(sourceCode, pageable);
    }

    @Override
    public Optional<CrawlingLog> findLatestBySourceCode(String sourceCode) {
        return springDataRepository.findFirstBySourceCodeOrderByExecutedAtDesc(sourceCode);
    }
}
