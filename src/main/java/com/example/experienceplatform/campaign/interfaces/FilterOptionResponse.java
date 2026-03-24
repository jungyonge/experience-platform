package com.example.experienceplatform.campaign.interfaces;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FilterOptionResponse {

    private final List<Option> sourceTypes;
    private final List<Option> categories;
    private final List<Option> statuses;
    private final List<Option> sortOptions;
    private final List<Option> regions;

    private FilterOptionResponse(List<Option> sourceTypes, List<Option> categories,
                                 List<Option> statuses, List<Option> sortOptions,
                                 List<Option> regions) {
        this.sourceTypes = sourceTypes;
        this.categories = categories;
        this.statuses = statuses;
        this.sortOptions = sortOptions;
        this.regions = regions;
    }

    public static FilterOptionResponse create(List<CrawlingSource> activeSources,
                                              List<String> regionValues) {
        Set<CampaignStatus> visibleStatuses = Set.of(
                CampaignStatus.RECRUITING, CampaignStatus.CLOSED);

        List<Option> sources = activeSources.stream()
                .map(s -> new Option(s.getCode(), s.getName()))
                .toList();

        List<Option> categories = Arrays.stream(CampaignCategory.values())
                .map(c -> new Option(c.name(), c.getDisplayName()))
                .toList();

        List<Option> statuses = Arrays.stream(CampaignStatus.values())
                .filter(visibleStatuses::contains)
                .map(s -> new Option(s.name(), s.getDisplayName()))
                .toList();

        List<Option> sortOptions = List.of(
                new Option("latest", "최신순"),
                new Option("deadline", "마감임박순"),
                new Option("popular", "모집인원순")
        );

        List<Option> regions = regionValues.stream()
                .map(r -> new Option(r, r))
                .toList();

        return new FilterOptionResponse(sources, categories, statuses, sortOptions, regions);
    }

    public List<Option> getSourceTypes() { return sourceTypes; }
    public List<Option> getCategories() { return categories; }
    public List<Option> getStatuses() { return statuses; }
    public List<Option> getSortOptions() { return sortOptions; }
    public List<Option> getRegions() { return regions; }

    public static class Option {
        private final String code;
        private final String name;

        public Option(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
    }
}
