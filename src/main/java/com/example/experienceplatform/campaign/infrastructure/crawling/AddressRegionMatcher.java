package com.example.experienceplatform.campaign.infrastructure.crawling;

import com.example.experienceplatform.campaign.domain.Region;
import com.example.experienceplatform.campaign.domain.RegionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AddressRegionMatcher {

    private final RegionRepository regionRepository;
    private volatile Map<String, List<Region>> regionsBySidoShort;

    public AddressRegionMatcher(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    private Map<String, List<Region>> getRegionsBySidoShort() {
        if (regionsBySidoShort == null) {
            synchronized (this) {
                if (regionsBySidoShort == null) {
                    List<Region> allRegions = regionRepository.findAllOrderBySidoAndSigungu();
                    regionsBySidoShort = allRegions.stream()
                            .collect(Collectors.groupingBy(Region::getSidoShort));
                }
            }
        }
        return regionsBySidoShort;
    }

    public Optional<Region> match(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        String[] parts = address.trim().split("\\s+");
        if (parts.length < 1) {
            return Optional.empty();
        }

        String sidoToken = parts[0];
        Map<String, List<Region>> regionsMap = getRegionsBySidoShort();
        List<Region> candidates = regionsMap.get(sidoToken);

        if (candidates == null) {
            for (Map.Entry<String, List<Region>> entry : regionsMap.entrySet()) {
                Region sample = entry.getValue().get(0);
                if (sample.getSido().startsWith(sidoToken)) {
                    candidates = entry.getValue();
                    break;
                }
            }
        }

        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        if (parts.length < 2) {
            return Optional.empty();
        }

        String sigunguToken = parts[1];
        for (Region region : candidates) {
            if (region.getSigungu().equals(sigunguToken) || sigunguToken.startsWith(region.getSigungu())) {
                return Optional.of(region);
            }
        }

        for (Region region : candidates) {
            if (region.getSigungu().startsWith(sigunguToken) || sigunguToken.contains(region.getSigungu())) {
                return Optional.of(region);
            }
        }

        return Optional.empty();
    }
}
