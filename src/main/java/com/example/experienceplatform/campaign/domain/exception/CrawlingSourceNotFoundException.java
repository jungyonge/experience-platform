package com.example.experienceplatform.campaign.domain.exception;

import com.example.experienceplatform.member.domain.exception.BusinessException;

public class CrawlingSourceNotFoundException extends BusinessException {

    public CrawlingSourceNotFoundException(String sourceCode) {
        super("CRAWLING_SOURCE_NOT_FOUND", "크롤링 소스를 찾을 수 없습니다: " + sourceCode);
    }

    public CrawlingSourceNotFoundException(Long id) {
        super("CRAWLING_SOURCE_NOT_FOUND", "크롤링 소스를 찾을 수 없습니다: ID=" + id);
    }
}
