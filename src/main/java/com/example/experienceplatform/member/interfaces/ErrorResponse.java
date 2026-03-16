package com.example.experienceplatform.member.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldError> errors;
    private final LocalDateTime timestamp;
    private final String path;

    private ErrorResponse(String code, String message, List<FieldError> errors, String path) {
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, null, path);
    }

    public static ErrorResponse ofValidation(List<FieldError> errors, String path) {
        return new ErrorResponse("VALIDATION_FAILED", "입력값이 올바르지 않습니다.", errors, path);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public record FieldError(String field, String message) {
    }
}
