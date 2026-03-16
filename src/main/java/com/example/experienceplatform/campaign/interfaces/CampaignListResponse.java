package com.example.experienceplatform.campaign.interfaces;

import com.example.experienceplatform.campaign.application.CampaignListInfo;

import java.util.List;

public class CampaignListResponse {

    private final List<CampaignItemResponse> campaigns;
    private final long totalCount;
    private final int totalPages;
    private final int currentPage;
    private final boolean hasNext;

    private CampaignListResponse(List<CampaignItemResponse> campaigns, long totalCount,
                                 int totalPages, int currentPage, boolean hasNext) {
        this.campaigns = campaigns;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.hasNext = hasNext;
    }

    public static CampaignListResponse from(CampaignListInfo info) {
        List<CampaignItemResponse> items = info.getCampaigns().stream()
                .map(CampaignItemResponse::from)
                .toList();
        return new CampaignListResponse(items, info.getTotalCount(),
                info.getTotalPages(), info.getCurrentPage(), info.isHasNext());
    }

    public List<CampaignItemResponse> getCampaigns() { return campaigns; }
    public long getTotalCount() { return totalCount; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public boolean isHasNext() { return hasNext; }
}
