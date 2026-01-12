package com.sotatek.order.api.dto;

import java.time.LocalDateTime;

/**
 * Standard error response DTO.
 */
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, LocalDateTime.now(), path);
    }
}
