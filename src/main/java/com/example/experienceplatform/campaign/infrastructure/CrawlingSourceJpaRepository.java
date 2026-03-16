package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.domain.CrawlingSourceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CrawlingSourceJpaRepository implements CrawlingSourceRepository {

    private final CrawlingSourceSpringDataJpaRepository springDataRepository;

    public CrawlingSourceJpaRepository(CrawlingSourceSpringDataJpaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CrawlingSource save(CrawlingSource source) {
        return springDataRepository.save(source);
    }

    @Override
    public Optional<CrawlingSource> findById(Long id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<CrawlingSource> findByCode(String code) {
        return springDataRepository.findByCode(code);
    }

    @Override
    public List<CrawlingSource> findAllActiveOrderByDisplayOrder() {
        return springDataRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public List<CrawlingSource> findAllOrderByDisplayOrder() {
        return springDataRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataRepository.existsByCode(code);
    }
}
