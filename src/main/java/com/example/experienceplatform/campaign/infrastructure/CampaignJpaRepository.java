package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class CampaignJpaRepository implements CampaignRepository {

    private final CampaignSpringDataJpaRepository springDataRepository;

    public CampaignJpaRepository(CampaignSpringDataJpaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return springDataRepository.save(campaign);
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<Campaign> findByCrawlingSourceAndOriginalId(CrawlingSource crawlingSource, String originalId) {
        return springDataRepository.findByCrawlingSourceAndOriginalId(crawlingSource, originalId);
    }

    @Override
    public Page<Campaign> searchByCondition(CampaignSearchCondition condition, Pageable pageable) {
        return springDataRepository.findAll(CampaignSpecification.withCondition(condition), pageable);
    }

    @Override
    public List<Campaign> findExpiredRecruitingCampaigns(LocalDate today) {
        return springDataRepository.findByStatusAndApplyEndDateBefore(CampaignStatus.RECRUITING, today);
    }

    @Override
    public long countByCrawlingSource(CrawlingSource crawlingSource) {
        return springDataRepository.countByCrawlingSource(crawlingSource);
    }

}
