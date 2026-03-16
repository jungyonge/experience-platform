package com.example.experienceplatform.member.interfaces;

import com.example.experienceplatform.campaign.domain.exception.CampaignNotFoundException;
import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingInProgressException;
import com.example.experienceplatform.campaign.interfaces.InvalidParameterException;
import com.example.experienceplatform.member.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(fieldErrors, request.getRequestURI()));
    }

    @ExceptionHandler({DuplicateEmailException.class, DuplicateNicknameException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(BusinessException e,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException e,
                                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException e,
                                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({InvalidRefreshTokenException.class, RefreshTokenExpiredException.class})
    public ResponseEntity<ErrorResponse> handleRefreshTokenError(BusinessException e,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(InvalidParameterException e,
                                                                 HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCampaignNotFound(CampaignNotFoundException e,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.example.experienceplatform.campaign.domain.exception.CrawlingSourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCrawlingSourceNotFound(
            com.example.experienceplatform.campaign.domain.exception.CrawlingSourceNotFoundException e,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException e,
                                                               HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({CurrentPasswordMismatchException.class, SamePasswordException.class,
            AlreadyWithdrawnException.class})
    public ResponseEntity<ErrorResponse> handleProfileError(BusinessException e,
                                                             HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CrawlingInProgressException.class)
    public ResponseEntity<ErrorResponse> handleCrawlingInProgress(CrawlingInProgressException e,
                                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CRAWLING_IN_PROGRESS", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingException.class)
    public ResponseEntity<ErrorResponse> handleCrawlingException(
            com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingException e,
            HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("CRAWLING_ERROR", e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.example.experienceplatform.campaign.domain.exception.DuplicateSourceCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSourceCode(
            com.example.experienceplatform.campaign.domain.exception.DuplicateSourceCodeException e,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.example.experienceplatform.campaign.domain.exception.CrawlerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCrawlerNotFound(
            com.example.experienceplatform.campaign.domain.exception.CrawlerNotFoundException e,
            HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                             HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_PARAMETER",
                        "잘못된 파라미터 형식입니다: " + e.getName(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e,
                                                        HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(e.getCode(), e.getMessage(), request.getRequestURI()));
    }
}
