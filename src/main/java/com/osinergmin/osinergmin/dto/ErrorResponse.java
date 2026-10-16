package com.osinergmin.osinergmin.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(Instant timestamp, int status, String error, String message, Map<String, String> campos) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message, Map<String, String> campos) {
        return new ErrorResponse(Instant.now(), status, error, message, campos);
    }
}
