package com.sotatek.order.api.dto;

import com.sotatek.order.domain.model.Order;
import com.sotatek.order.domain.model.OrderItem;
import com.sotatek.order.domain.model.OrderStatus;
import com.sotatek.order.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Order.
 */
public record OrderResponse(
        Long id,
        Long memberId,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        OrderStatus status,
        Long paymentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static OrderResponse fromDomain(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::fromDomain)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getMemberId(),
                items,
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getPaymentId(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal) {
        public static OrderItemResponse fromDomain(OrderItem item) {
            return new OrderItemResponse(
                    item.productId(),
                    item.productName(),
                    item.unitPrice(),
                    item.quantity(),
                    item.getSubtotal());
        }
    }
}
