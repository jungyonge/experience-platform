package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CrawlerRegistry {

    private final Map<String, CampaignCrawler> crawlerMap;

    public CrawlerRegistry(List<CampaignCrawler> crawlers) {
        this.crawlerMap = crawlers.stream()
                .collect(Collectors.toMap(CampaignCrawler::getCrawlerType, Function.identity()));
    }

    public Optional<CampaignCrawler> findByCrawlerType(String crawlerType) {
        return Optional.ofNullable(crawlerMap.get(crawlerType));
    }

    public List<String> getAvailableCrawlerTypes() {
        return crawlerMap.keySet().stream().sorted().toList();
    }
}
