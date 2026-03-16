package com.example.experienceplatform.campaign.infrastructure.crawling;

public class CrawlingException extends RuntimeException {

    public CrawlingException(String message) {
        super(message);
    }

    public CrawlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
