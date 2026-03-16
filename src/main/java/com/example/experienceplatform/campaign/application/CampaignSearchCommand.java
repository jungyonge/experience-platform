package com.example.experienceplatform.campaign.application;

import java.util.Set;

public class CampaignSearchCommand {

    private final String keyword;
    private final Set<String> sourceTypes;
    private final Set<String> categories;
    private final String status;
    private final int page;
    private final int size;
    private final String sort;

    public CampaignSearchCommand(String keyword, Set<String> sourceTypes,
                                 Set<String> categories, String status,
                                 int page, int size, String sort) {
        this.keyword = keyword;
        this.sourceTypes = sourceTypes;
        this.categories = categories;
        this.status = status;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public String getKeyword() {
        return keyword;
    }

    public Set<String> getSourceTypes() {
        return sourceTypes;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public String getStatus() {
        return status;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSort() {
        return sort;
    }
}
