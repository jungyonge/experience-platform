package com.example.experienceplatform.campaign.domain.exception;

import com.example.experienceplatform.member.domain.exception.BusinessException;

public class DuplicateSourceCodeException extends BusinessException {

    public DuplicateSourceCodeException(String code) {
        super("DUPLICATE_SOURCE_CODE", "이미 존재하는 소스 코드입니다: " + code);
    }
}
