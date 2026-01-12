package com.sotatek.order.infrastructure.persistence;

import com.sotatek.order.domain.model.Order;
import com.sotatek.order.domain.model.OrderItem;

import java.util.stream.Collectors;

/**
 * Mapper between Domain Order and JPA OrderEntity.
 */
public class OrderMapper {

    private OrderMapper() {
        // Utility class
    }

    public static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setMemberId(order.getMemberId());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setPaymentMethod(order.getPaymentMethod());
        entity.setStatus(order.getStatus());
        entity.setPaymentId(order.getPaymentId());
        entity.setVersion(order.getVersion());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        // Map items
        for (OrderItem item : order.getItems()) {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setProductId(item.productId());
            itemEntity.setProductName(item.productName());
            itemEntity.setUnitPrice(item.unitPrice());
            itemEntity.setQuantity(item.quantity());
            entity.addItem(itemEntity);
        }

        return entity;
    }

    public static Order toDomain(OrderEntity entity) {
        var items = entity.getItems().stream()
                .map(itemEntity -> new OrderItem(
                        itemEntity.getProductId(),
                        itemEntity.getProductName(),
                        itemEntity.getUnitPrice(),
                        itemEntity.getQuantity()))
                .collect(Collectors.toList());

        return Order.reconstitute(
                entity.getId(),
                entity.getMemberId(),
                items,
                entity.getTotalAmount(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getPaymentId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
