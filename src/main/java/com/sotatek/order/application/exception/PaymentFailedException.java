package com.sotatek.order.application.exception;

/**
 * Exception when payment fails.
 */
public class PaymentFailedException extends RuntimeException {

    private final Long orderId;
    private final String reason;

    public PaymentFailedException(Long orderId, String reason) {
        super("Payment failed for order " + orderId + ": " + reason);
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}
