package com.example.experienceplatform.campaign.domain.bookmark;

import java.time.LocalDateTime;

public interface BookmarkRepository {

    long count();

    long countByMemberId(Long memberId);

    long countByCampaignId(Long campaignId);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    void deleteByCampaignId(Long campaignId);

    Bookmark save(Bookmark bookmark);
}
