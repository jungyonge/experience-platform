package com.example.experienceplatform.campaign.interfaces;

public class InvalidParameterException extends RuntimeException {

    private final String code;

    public InvalidParameterException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
