package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.Campaign;
import com.example.experienceplatform.campaign.domain.CampaignSearchCondition;
import org.springframework.data.jpa.domain.Specification;

public final class CampaignSpecification {

    private CampaignSpecification() {
    }

    public static Specification<Campaign> withCondition(CampaignSearchCondition condition) {
        Specification<Campaign> spec = Specification.where(null);

        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            String keyword = condition.getKeyword().trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + keyword + "%"));
        }

        if (condition.getSourceCodes() != null && !condition.getSourceCodes().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("crawlingSource").get("code").in(condition.getSourceCodes()));
        }

        if (condition.getCategories() != null && !condition.getCategories().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("category").in(condition.getCategories()));
        }

        if (condition.getStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), condition.getStatus()));
        }

        if (condition.getRegionId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("region").get("id"), condition.getRegionId()));
        } else if (condition.getSido() != null && !condition.getSido().isBlank()) {
            String sido = condition.getSido().trim();
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("region").get("sido"), sido));
        }

        return spec;
    }
}
