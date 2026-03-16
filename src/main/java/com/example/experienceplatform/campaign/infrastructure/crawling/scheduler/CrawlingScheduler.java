package com.example.experienceplatform.campaign.infrastructure.crawling.scheduler;

import com.example.experienceplatform.campaign.application.crawling.CrawlingOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "crawling.enabled", havingValue = "true")
public class CrawlingScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrawlingScheduler.class);

    private final CrawlingOrchestrator orchestrator;

    public CrawlingScheduler(CrawlingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("애플리케이션 기동 완료 - 초기 크롤링 시작");
        scheduledCrawl();
    }

    @Scheduled(cron = "${crawling.schedule-cron}")
    public void scheduledCrawl() {
        log.info("스케줄 크롤링 시작");
        try {
            orchestrator.executeAll();
            log.info("스케줄 크롤링 완료");
        } catch (Exception e) {
            log.error("스케줄 크롤링 실패: {}", e.getMessage());
        }
    }
}
