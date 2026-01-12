package com.sotatek.order.domain.model;

/**
 * Order status enum - 4 possible states.
 * See: docs/01-requirements.md FR-04
 */
public enum OrderStatus {
    /**
     * Payment completed successfully
     */
    CONFIRMED,

    /**
     * Payment is being processed
     */
    PENDING,

    /**
     * Validation or payment failed
     */
    FAILED,

    /**
     * Order cancelled by user
     */
    CANCELLED
}
