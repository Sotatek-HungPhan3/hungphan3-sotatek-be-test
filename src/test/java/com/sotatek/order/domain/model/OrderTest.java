package com.sotatek.order.domain.model;

import com.sotatek.order.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    @DisplayName("UT-01: Create new order should initialize correctly")
    void createOrder_ShouldInitializeCorrectly() {
        // Arrange
        Long memberId = 1L;
        OrderItem item = new OrderItem(101L, "Test Product", new BigDecimal("100.00"), 2);
        List<OrderItem> items = List.of(item);
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        // Act
        Order order = Order.create(memberId, items, paymentMethod);

        // Assert
        assertNotNull(order);
        assertEquals(memberId, order.getMemberId());
        assertEquals(OrderStatus.PENDING, order.getStatus()); // Initially PENDING until processed
        assertEquals(1, order.getItems().size());
        assertEquals(new BigDecimal("200.00"), order.getTotalAmount());
    }

    @Test
    @DisplayName("UT-02: Cancel PENDING order should succeed")
    void cancel_PendingOrder_ShouldSucceed() {
        // Arrange
        Order order = createTestOrder();
        // Default is PENDING

        // Act
        order.cancel();

        // Assert
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("UT-03: Cancel FAILED order should succeed")
    void cancel_FailedOrder_ShouldSucceed() {
        // Arrange
        Order order = createTestOrder();
        order.markAsFailed();

        // Act
        order.cancel();

        // Assert
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("UT-04: Cancel CONFIRMED order should throw exception")
    void cancel_ConfirmedOrder_ShouldThrowException() {
        // Arrange
        Order order = createTestOrder();
        order.markAsConfirmed(123L);

        // Act & Assert
        assertThrows(DomainException.class, order::cancel);
    }

    @Test
    @DisplayName("UT-05: Cancel CANCELLED order should throw exception")
    void cancel_CancelledOrder_ShouldThrowException() {
        // Arrange
        Order order = createTestOrder();
        order.cancel();

        // Act & Assert
        assertThrows(DomainException.class, order::cancel);
    }

    @Test
    @DisplayName("UT-06: Create valid OrderItem should succeed")
    void createOrderItem_Valid_ShouldSucceed() {
        OrderItem item = new OrderItem(101L, "Product", new BigDecimal("10.00"), 5);
        assertNotNull(item);
        assertEquals(101L, item.productId());
        assertEquals(new BigDecimal("50.00"), item.getSubtotal());
    }

    @Test
    @DisplayName("UT-07: Create OrderItem with qty <= 0 should throw exception")
    void createOrderItem_InvalidQty_ShouldThrowException() {
        assertThrows(DomainException.class, () -> new OrderItem(101L, "Product", new BigDecimal("10.00"), 0));
    }

    @Test
    @DisplayName("UT-07bis: Create order with empty items should throw exception")
    void createOrder_EmptyItems_ShouldThrowException() {
        assertThrows(DomainException.class, () -> Order.create(1L, Collections.emptyList(), PaymentMethod.CREDIT_CARD));
    }

    private Order createTestOrder() {
        OrderItem item = new OrderItem(101L, "Test Product", new BigDecimal("100.00"), 1);
        return Order.create(1L, List.of(item), PaymentMethod.CREDIT_CARD);
    }
}
