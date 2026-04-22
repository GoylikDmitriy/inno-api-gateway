package com.goylik.api_gateway.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        Map<String, String> details
) {
    public static final String MESSAGE_KEY = "message";

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                Map.of(MESSAGE_KEY, message)
        );
    }
}

