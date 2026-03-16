package com.example.experienceplatform.campaign.infrastructure.bookmark;

import com.example.experienceplatform.campaign.domain.bookmark.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

interface BookmarkSpringDataJpaRepository extends JpaRepository<Bookmark, Long> {

    long countByMemberId(Long memberId);

    long countByCampaignId(Long campaignId);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    void deleteByCampaignId(Long campaignId);
}
