package com.example.experienceplatform.campaign.infrastructure.bookmark;

import com.example.experienceplatform.campaign.domain.bookmark.Bookmark;
import com.example.experienceplatform.campaign.domain.bookmark.BookmarkRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class BookmarkJpaRepository implements BookmarkRepository {

    private final BookmarkSpringDataJpaRepository springDataRepository;

    public BookmarkJpaRepository(BookmarkSpringDataJpaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public long count() {
        return springDataRepository.count();
    }

    @Override
    public long countByMemberId(Long memberId) {
        return springDataRepository.countByMemberId(memberId);
    }

    @Override
    public long countByCampaignId(Long campaignId) {
        return springDataRepository.countByCampaignId(campaignId);
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime dateTime) {
        return springDataRepository.countByCreatedAtAfter(dateTime);
    }

    @Override
    public void deleteByCampaignId(Long campaignId) {
        springDataRepository.deleteByCampaignId(campaignId);
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        return springDataRepository.save(bookmark);
    }
}
