package com.example.experienceplatform.campaign.domain.exception;

import com.example.experienceplatform.member.domain.exception.BusinessException;

public class CrawlerNotFoundException extends BusinessException {

    public CrawlerNotFoundException(String crawlerType) {
        super("CRAWLER_NOT_FOUND", "매칭되는 크롤러를 찾을 수 없습니다: " + crawlerType);
    }
}
