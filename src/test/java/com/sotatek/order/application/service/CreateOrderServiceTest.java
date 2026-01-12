package com.sotatek.order.application.service;

import com.sotatek.order.application.dto.*;
import com.sotatek.order.application.exception.MemberValidationException;
import com.sotatek.order.application.exception.PaymentFailedException;
import com.sotatek.order.application.exception.ProductValidationException;
import com.sotatek.order.application.port.out.*;
import com.sotatek.order.domain.model.Order;
import com.sotatek.order.domain.model.OrderStatus;
import com.sotatek.order.domain.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

        @Mock
        private OrderRepositoryPort orderRepository;
        @Mock
        private MemberClientPort memberClient;
        @Mock
        private ProductClientPort productClient;
        @Mock
        private PaymentClientPort paymentClient;

        private CreateOrderService createOrderService;

        @BeforeEach
        void setUp() {
                createOrderService = new CreateOrderService(
                                orderRepository, memberClient, productClient, paymentClient);
        }

        @Test
        @DisplayName("UT-08: Execute valid order creation (Success Payment)")
        void execute_ValidOrder_SuccessPayment_ShouldCreateConfirmedOrder() {
                // Arrange
                Long memberId = 1L;
                Long productId = 101L;
                PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

                // Mock Member
                when(memberClient.getMember(memberId)).thenReturn(Optional.of(
                                new MemberDto(memberId, "Test User", "test@test.com", "ACTIVE", "GOLD")));

                // Mock Product & Stock
                when(productClient.getProduct(productId)).thenReturn(Optional.of(
                                new ProductDto(productId, "Test Product", new BigDecimal("100.00"), "AVAILABLE")));
                when(productClient.getStock(productId)).thenReturn(Optional.of(
                                new ProductStockDto(productId, 100, 0, 100)));

                // Mock Repository (save)
                when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        return order; // Return same object
                });

                // Mock Payment
                when(paymentClient.createPayment(any(PaymentRequestDto.class))).thenReturn(
                                new PaymentResponseDto(1L, 1L, new BigDecimal("200.00"), "COMPLETED", "TXN-1",
                                                LocalDateTime.now()));

                List<CreateOrderService.OrderItemRequest> items = List.of(
                                new CreateOrderService.OrderItemRequest(productId, 2));

                // Act
                Order result = createOrderService.execute(memberId, items, paymentMethod);

                // Assert
                assertNotNull(result);
                assertEquals(OrderStatus.CONFIRMED, result.getStatus());
                verify(orderRepository, times(2)).save(any(Order.class)); // 1. Initial save, 2. Update status
                verify(paymentClient).createPayment(any(PaymentRequestDto.class));
        }

        @Test
        @DisplayName("UT-10: Execute with invalid member should throw exception")
        void execute_InvalidMember_ShouldThrowException() {
                // Arrange
                Long memberId = 999L;
                when(memberClient.getMember(memberId)).thenReturn(Optional.empty());

                List<CreateOrderService.OrderItemRequest> items = List.of(
                                new CreateOrderService.OrderItemRequest(101L, 1));

                // Act & Assert
                assertThrows(MemberValidationException.class,
                                () -> createOrderService.execute(memberId, items, PaymentMethod.CREDIT_CARD));
                verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-12: Execute with insufficient stock should throw exception")
        void execute_InsufficientStock_ShouldThrowException() {
                // Arrange
                Long memberId = 1L;
                Long productId = 101L;

                when(memberClient.getMember(memberId)).thenReturn(Optional.of(
                                new MemberDto(memberId, "Test User", "test@test.com", "ACTIVE", "GOLD")));

                when(productClient.getProduct(productId)).thenReturn(Optional.of(
                                new ProductDto(productId, "Product", new BigDecimal("100.00"), "AVAILABLE")));

                // Stock only 1
                when(productClient.getStock(productId)).thenReturn(Optional.of(
                                new ProductStockDto(productId, 10, 9, 1)));

                List<CreateOrderService.OrderItemRequest> items = List.of(
                                new CreateOrderService.OrderItemRequest(productId, 2) // Request 2
                );

                // Act & Assert
                assertThrows(ProductValidationException.class,
                                () -> createOrderService.execute(memberId, items, PaymentMethod.CREDIT_CARD));
                verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-13: Payment Failure should mark order as FAILED")
        void execute_PaymentFailure_ShouldMarkOrderAsFailed() {
                // Arrange
                Long memberId = 1L;
                Long productId = 101L;

                when(memberClient.getMember(memberId))
                                .thenReturn(Optional.of(new MemberDto(memberId, "User", "e", "ACTIVE", "G")));
                when(productClient.getProduct(productId))
                                .thenReturn(Optional.of(
                                                new ProductDto(productId, "P", new BigDecimal("100"), "AVAILABLE")));
                when(productClient.getStock(productId))
                                .thenReturn(Optional.of(new ProductStockDto(productId, 10, 0, 10)));
                when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

                // Mock Payment Failure
                doThrow(new PaymentFailedException(1L, "Failed")).when(paymentClient).createPayment(any());

                List<CreateOrderService.OrderItemRequest> items = List
                                .of(new CreateOrderService.OrderItemRequest(productId, 1));

                // Act & Assert
                assertThrows(PaymentFailedException.class,
                                () -> createOrderService.execute(memberId, items, PaymentMethod.CREDIT_CARD));

                // Verify order saved as FAILED
                ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
                verify(orderRepository, times(2)).save(orderCaptor.capture());
                Order savedOrder = orderCaptor.getAllValues().get(1); // 2nd save is the status update
                assertEquals(OrderStatus.FAILED, savedOrder.getStatus());
        }
}
