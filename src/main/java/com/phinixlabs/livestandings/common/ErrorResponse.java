package com.phinixlabs.livestandings.common;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String message,
        String code,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, message, code, Map.of(), Instant.now());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(
                false,
                message,
                code,
                fieldErrors == null ? Map.of() : fieldErrors,
                Instant.now()
        );
    }
}
