package com.example.experienceplatform.campaign.interfaces;

import com.example.experienceplatform.campaign.application.CampaignSearchCommand;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CampaignSearchRequest {

    private static final Set<String> VALID_SORTS = Set.of("latest", "deadline", "popular");
    private static final int MAX_SIZE = 50;

    private String keyword;
    private String sourceTypes;
    private String categories;
    private String status;
    private String region;
    private int page = 0;
    private int size = 12;
    private String sort = "latest";

    public void validate() {
        if (page < 0) {
            throw new InvalidParameterException("INVALID_PAGE_NUMBER", "페이지 번호는 0 이상이어야 합니다.");
        }
        if (size > MAX_SIZE) {
            throw new InvalidParameterException("INVALID_PAGE_SIZE", "페이지 크기는 최대 " + MAX_SIZE + "입니다.");
        }
        if (!VALID_SORTS.contains(sort)) {
            throw new InvalidParameterException("INVALID_SORT_VALUE", "허용되지 않는 정렬 값입니다: " + sort);
        }
    }

    public CampaignSearchCommand toCommand() {
        return new CampaignSearchCommand(
                keyword,
                parseCommaSeparated(sourceTypes),
                parseCommaSeparated(categories),
                status,
                region,
                page,
                size,
                sort
        );
    }

    private Set<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSourceTypes() { return sourceTypes; }
    public void setSourceTypes(String sourceTypes) { this.sourceTypes = sourceTypes; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
