package com.sotatek.order.domain.exception;

/**
 * Thrown when an order is not found.
 */
public class OrderNotFoundException extends DomainException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
