package com.example.experienceplatform.campaign.infrastructure.crawling;

public class CrawlingInProgressException extends RuntimeException {

    public CrawlingInProgressException() {
        super("크롤링이 이미 실행 중입니다.");
    }
}
