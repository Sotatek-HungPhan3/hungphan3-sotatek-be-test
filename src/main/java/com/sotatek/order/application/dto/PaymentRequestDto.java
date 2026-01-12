package com.sotatek.order.application.dto;

import java.math.BigDecimal;

/**
 * Payment Request DTO for external Payment Service.
 */
public record PaymentRequestDto(
        Long orderId,
        BigDecimal amount,
        String paymentMethod) {
}
