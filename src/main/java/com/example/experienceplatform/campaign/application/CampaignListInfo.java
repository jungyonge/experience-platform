package com.example.experienceplatform.campaign.application;

import java.util.List;

public class CampaignListInfo {

    private final List<CampaignSummary> campaigns;
    private final long totalCount;
    private final int totalPages;
    private final int currentPage;
    private final boolean hasNext;

    public CampaignListInfo(List<CampaignSummary> campaigns, long totalCount,
                            int totalPages, int currentPage, boolean hasNext) {
        this.campaigns = campaigns;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.hasNext = hasNext;
    }

    public List<CampaignSummary> getCampaigns() { return campaigns; }
    public long getTotalCount() { return totalCount; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public boolean isHasNext() { return hasNext; }
}
