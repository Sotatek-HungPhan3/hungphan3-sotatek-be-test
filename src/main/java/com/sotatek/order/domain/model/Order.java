package com.sotatek.order.domain.model;

import com.sotatek.order.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order Aggregate Root.
 * Contains state machine logic for order lifecycle.
 * See: docs/00-problem-understanding.md - Order State Machine
 */
public class Order {

    private Long id;
    private Long memberId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private Long paymentId;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Private constructor - use factory method
    private Order() {
        this.items = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a new order.
     */
    public static Order create(Long memberId, List<OrderItem> items, PaymentMethod paymentMethod) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Member ID must be positive");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        Order order = new Order();
        order.memberId = memberId;
        order.items = new ArrayList<>(items);
        order.paymentMethod = paymentMethod;
        order.totalAmount = order.calculateTotalAmount();
        order.status = OrderStatus.PENDING; // Initial state before payment result
        return order;
    }

    /**
     * Reconstruct order from persistence.
     */
    public static Order reconstitute(
            Long id,
            Long memberId,
            List<OrderItem> items,
            BigDecimal totalAmount,
            PaymentMethod paymentMethod,
            OrderStatus status,
            Long paymentId,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        Order order = new Order();
        order.id = id;
        order.memberId = memberId;
        order.items = new ArrayList<>(items);
        order.totalAmount = totalAmount;
        order.paymentMethod = paymentMethod;
        order.status = status;
        order.paymentId = paymentId;
        order.version = version;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    // ==================== State Transitions ====================

    /**
     * Mark order as CONFIRMED (payment succeeded).
     */
    public void markAsConfirmed(Long paymentId) {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot confirm order",
                    this.status.name(),
                    OrderStatus.CONFIRMED.name());
        }
        this.status = OrderStatus.CONFIRMED;
        this.paymentId = paymentId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark order as PENDING (payment processing).
     */
    public void markAsPending(Long paymentId) {
        this.status = OrderStatus.PENDING;
        this.paymentId = paymentId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark order as FAILED (validation or payment failed).
     */
    public void markAsFailed() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancel the order.
     * Only PENDING and FAILED orders can be cancelled.
     * CONFIRMED orders cannot be cancelled (already paid).
     * CANCELLED → CANCELLED is idempotent.
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            // Idempotent - already cancelled
            return;
        }
        if (this.status == OrderStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel confirmed order (already paid)",
                    this.status.name(),
                    OrderStatus.CANCELLED.name());
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Helpers ====================

    private BigDecimal calculateTotalAmount() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== Getters ====================

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Package-private setter for persistence
    void setId(Long id) {
        this.id = id;
    }

    void setVersion(Long version) {
        this.version = version;
    }
}
