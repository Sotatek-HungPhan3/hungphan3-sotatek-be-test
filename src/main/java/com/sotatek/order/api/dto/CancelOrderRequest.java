package com.sotatek.order.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for cancelling an order.
 */
public record CancelOrderRequest(
        @NotNull(message = "Status is required") String status) {
    public boolean isCancelRequest() {
        return "CANCELLED".equalsIgnoreCase(status);
    }
}
