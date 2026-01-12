package com.sotatek.order.application.dto;

import java.math.BigDecimal;

/**
 * Product DTO from external Product Service.
 */
public record ProductDto(
        Long id,
        String name,
        BigDecimal price,
        String status) {
    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }

    public boolean isDiscontinued() {
        return "DISCONTINUED".equals(status);
    }
}
