package com.example.experienceplatform.campaign.infrastructure.crawling;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class CrawlingDelayHandler {

    private final CrawlingProperties properties;

    public CrawlingDelayHandler(CrawlingProperties properties) {
        this.properties = properties;
    }

    public void delay() {
        try {
            int delayMs = ThreadLocalRandom.current().nextInt(
                    properties.getDelayMinMs(), properties.getDelayMaxMs() + 1);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
