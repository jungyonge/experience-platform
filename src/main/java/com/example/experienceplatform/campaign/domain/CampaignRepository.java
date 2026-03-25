package com.example.experienceplatform.campaign.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository {

    Campaign save(Campaign campaign);

    Optional<Campaign> findById(Long id);

    Optional<Campaign> findByCrawlingSourceAndOriginalId(CrawlingSource crawlingSource, String originalId);

    Page<Campaign> searchByCondition(CampaignSearchCondition condition, Pageable pageable);

    List<Campaign> findExpiredRecruitingCampaigns(LocalDate today);

    long countByCrawlingSource(CrawlingSource crawlingSource);
}
