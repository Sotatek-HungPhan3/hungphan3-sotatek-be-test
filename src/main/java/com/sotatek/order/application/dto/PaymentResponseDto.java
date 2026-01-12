package com.sotatek.order.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Response DTO from external Payment Service.
 */
public record PaymentResponseDto(
        Long id,
        Long orderId,
        BigDecimal amount,
        String status,
        String transactionId,
        LocalDateTime createdAt) {
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}
