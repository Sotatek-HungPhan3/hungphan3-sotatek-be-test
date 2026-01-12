package com.sotatek.order.application.service;

import com.sotatek.order.application.port.out.OrderRepositoryPort;
import com.sotatek.order.domain.exception.OrderNotFoundException;
import com.sotatek.order.domain.model.Order;
import com.sotatek.order.domain.model.OrderItem;
import com.sotatek.order.domain.model.OrderStatus;
import com.sotatek.order.domain.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    private CancelOrderService cancelOrderService;

    @BeforeEach
    void setUp() {
        cancelOrderService = new CancelOrderService(orderRepository);
    }

    @Test
    @DisplayName("UT-14: Cancel existing order should succeed")
    void execute_ExistingOrder_ShouldUpdateStatusToCancelled() {
        // Arrange
        Long orderId = 1L;
        Order order = createTestOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Order result = cancelOrderService.execute(orderId);

        // Assert
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("UT-15: Cancel non-existent order should throw exception")
    void execute_NonExistentOrder_ShouldThrowException() {
        // Arrange
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> cancelOrderService.execute(orderId));
        verify(orderRepository, never()).save(any());
    }

    private Order createTestOrder(Long id) {
        OrderItem item = new OrderItem(101L, "Test Product", new BigDecimal("100.00"), 1);
        Order order = Order.create(1L, List.of(item), PaymentMethod.CREDIT_CARD);
        // Set ID via reflection or package-private if needed, but here findById handles
        // it
        return order;
    }
}
