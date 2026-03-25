package com.example.experienceplatform.campaign.application;

import com.example.experienceplatform.campaign.domain.*;
import com.example.experienceplatform.campaign.domain.exception.CampaignNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public CampaignListInfo searchCampaigns(CampaignSearchCommand command) {
        Set<String> sourceCodes = command.getSourceTypes();
        if (sourceCodes == null) {
            sourceCodes = Set.of();
        }

        Set<CampaignCategory> categories = parseEnums(command.getCategories(), CampaignCategory.class);
        CampaignStatus status = parseEnum(command.getStatus(), CampaignStatus.class);

        String keyword = command.getKeyword();
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        String sido = command.getSido();
        if (sido != null) {
            sido = sido.trim();
            if (sido.isEmpty()) {
                sido = null;
            }
        }

        CampaignSearchCondition condition = new CampaignSearchCondition(
                keyword, sourceCodes, categories, status, command.getRegionId(), sido);

        Sort sort = resolveSort(command.getSort());
        Pageable pageable = PageRequest.of(command.getPage(), command.getSize(), sort);

        Page<Campaign> page = campaignRepository.searchByCondition(condition, pageable);

        List<CampaignSummary> summaries = page.getContent().stream()
                .map(CampaignSummary::from)
                .toList();

        return new CampaignListInfo(
                summaries,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.hasNext()
        );
    }

    public CampaignDetail getDetail(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new CampaignNotFoundException(id));
        return CampaignDetail.from(campaign);
    }

    private Sort resolveSort(String sortValue) {
        if (sortValue == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortValue) {
            case "deadline" -> Sort.by(Sort.Order.asc("applyEndDate").nullsLast());
            case "popular" -> Sort.by(Sort.Order.desc("recruitCount").nullsLast());
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private <E extends Enum<E>> Set<E> parseEnums(Set<String> values, Class<E> enumClass) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<E> result = new HashSet<>();
        for (String value : values) {
            try {
                result.add(Enum.valueOf(enumClass, value.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
