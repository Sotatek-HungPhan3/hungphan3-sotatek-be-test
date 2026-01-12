package com.sotatek.order.application.exception;

/**
 * Exception when product validation fails.
 */
public class ProductValidationException extends RuntimeException {

    private final Long productId;
    private final String reason;

    public ProductValidationException(Long productId, String reason) {
        super(reason);
        this.productId = productId;
        this.reason = reason;
    }

    public Long getProductId() {
        return productId;
    }

    public String getReason() {
        return reason;
    }
}
