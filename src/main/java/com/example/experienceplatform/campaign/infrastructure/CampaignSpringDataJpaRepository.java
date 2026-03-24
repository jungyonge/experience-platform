package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.Campaign;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface CampaignSpringDataJpaRepository
        extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

    Optional<Campaign> findByCrawlingSourceAndOriginalId(CrawlingSource crawlingSource, String originalId);

    List<Campaign> findByStatusAndApplyEndDateBefore(CampaignStatus status, LocalDate date);

    long countByCrawlingSource(CrawlingSource crawlingSource);

    @Query("SELECT DISTINCT c.address FROM Campaign c WHERE c.address IS NOT NULL AND c.address <> '' ORDER BY c.address")
    List<String> findDistinctAddresses();
}
