package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class DetailPageEnricher {

    private static final Logger log = LoggerFactory.getLogger(DetailPageEnricher.class);

    private final CrawlingProperties properties;
    private final JsoupClient jsoupClient;

    public DetailPageEnricher(CrawlingProperties properties, JsoupClient jsoupClient) {
        this.properties = properties;
        this.jsoupClient = jsoupClient;
    }

    public List<CrawledCampaign> enrich(List<CrawledCampaign> campaigns,
                                         BiFunction<CrawledCampaign, Document, CrawledCampaign> parser) {
        if (!properties.isDetailFetchEnabled()) {
            return campaigns;
        }
        List<CrawledCampaign> enriched = new ArrayList<>();
        for (CrawledCampaign campaign : campaigns) {
            try {
                Thread.sleep(properties.getDetailFetchDelayMs());
                Document detailDoc = jsoupClient.fetch(campaign.getOriginalUrl());
                CrawledCampaign enrichedCampaign = parser.apply(campaign, detailDoc);
                enriched.add(enrichedCampaign != null ? enrichedCampaign : campaign);
            } catch (Exception e) {
                log.warn("상세페이지 fetch 실패 {}: {}", campaign.getOriginalId(), e.getMessage());
                enriched.add(campaign);
            }
        }
        return enriched;
    }

    public static String coalesce(String existing, String fallback) {
        return existing != null ? existing : fallback;
    }

    public static Integer coalesce(Integer existing, Integer fallback) {
        return existing != null ? existing : fallback;
    }

    public static java.time.LocalDate coalesce(java.time.LocalDate existing, java.time.LocalDate fallback) {
        return existing != null ? existing : fallback;
    }
}
