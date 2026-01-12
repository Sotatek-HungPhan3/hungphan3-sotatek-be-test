package com.sotatek.order.application.dto;

/**
 * Product Stock DTO from external Product Service.
 */
public record ProductStockDto(
        Long productId,
        int quantity,
        int reservedQuantity,
        int availableQuantity) {
    public boolean hasAvailableStock(int requestedQuantity) {
        return availableQuantity >= requestedQuantity;
    }
}
