package com.example.experienceplatform.campaign.interfaces;

import com.example.experienceplatform.campaign.domain.CampaignCategory;
import com.example.experienceplatform.campaign.domain.CampaignStatus;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.domain.Region;

import java.util.*;
import java.util.stream.Collectors;

public class FilterOptionResponse {

    private final List<Option> sourceTypes;
    private final List<Option> categories;
    private final List<Option> statuses;
    private final List<Option> sortOptions;
    private final List<RegionGroup> regions;

    private FilterOptionResponse(List<Option> sourceTypes, List<Option> categories,
                                 List<Option> statuses, List<Option> sortOptions,
                                 List<RegionGroup> regions) {
        this.sourceTypes = sourceTypes;
        this.categories = categories;
        this.statuses = statuses;
        this.sortOptions = sortOptions;
        this.regions = regions;
    }

    public static FilterOptionResponse create(List<CrawlingSource> activeSources,
                                              List<Region> regionEntities) {
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

        Map<String, List<Region>> grouped = regionEntities.stream()
                .collect(Collectors.groupingBy(Region::getSido, LinkedHashMap::new, Collectors.toList()));

        List<RegionGroup> regions = grouped.entrySet().stream()
                .map(entry -> new RegionGroup(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(r -> new Option(String.valueOf(r.getId()), r.getSigungu()))
                                .toList()))
                .toList();

        return new FilterOptionResponse(sources, categories, statuses, sortOptions, regions);
    }

    public List<Option> getSourceTypes() { return sourceTypes; }
    public List<Option> getCategories() { return categories; }
    public List<Option> getStatuses() { return statuses; }
    public List<Option> getSortOptions() { return sortOptions; }
    public List<RegionGroup> getRegions() { return regions; }

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

    public static class RegionGroup {
        private final String sido;
        private final List<Option> sigungus;

        public RegionGroup(String sido, List<Option> sigungus) {
            this.sido = sido;
            this.sigungus = sigungus;
        }

        public String getSido() { return sido; }
        public List<Option> getSigungus() { return sigungus; }
    }
}
