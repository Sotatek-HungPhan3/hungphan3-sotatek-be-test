package com.sotatek.order.domain.model;

import java.math.BigDecimal;

/**
 * OrderItem - Value Object representing a line item in an order.
 * Immutable: captures product snapshot at time of order creation.
 */
public record OrderItem(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity) {
    public OrderItem {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
    }

    /**
     * Calculate subtotal for this item.
     */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
