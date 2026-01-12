package com.sotatek.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for creating an order.
 */
public record CreateOrderRequest(
        @NotNull(message = "Member ID is required") Long memberId,

        @NotEmpty(message = "Order must have at least one item") @Valid List<OrderItemRequest> items,

        @NotNull(message = "Payment method is required") String paymentMethod) {
}
