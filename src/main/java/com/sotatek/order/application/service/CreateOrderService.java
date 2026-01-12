package com.sotatek.order.application.service;

import com.sotatek.order.application.dto.*;
import com.sotatek.order.application.exception.*;
import com.sotatek.order.application.port.out.*;
import com.sotatek.order.domain.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case: Create Order.
 * Flow: Validate Member → Validate Products/Stock → Create Order → Call Payment
 * → Update Status
 */
@Service
@Transactional
public class CreateOrderService {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderService.class);

    private final OrderRepositoryPort orderRepository;
    private final MemberClientPort memberClient;
    private final ProductClientPort productClient;
    private final PaymentClientPort paymentClient;

    public CreateOrderService(
            OrderRepositoryPort orderRepository,
            MemberClientPort memberClient,
            ProductClientPort productClient,
            PaymentClientPort paymentClient) {
        this.orderRepository = orderRepository;
        this.memberClient = memberClient;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
    }

    /**
     * Create a new order.
     *
     * @param memberId      the member ID
     * @param itemRequests  list of items (productId, quantity)
     * @param paymentMethod the payment method
     * @return created order
     */
    public Order execute(Long memberId, List<OrderItemRequest> itemRequests, PaymentMethod paymentMethod) {
        log.info("Creating order for memberId={}, items={}", memberId, itemRequests.size());

        // 1. Validate Member
        validateMember(memberId);

        // 2. Validate Products and Stock, build OrderItems
        List<OrderItem> orderItems = validateAndBuildOrderItems(itemRequests);

        // 3. Create Order
        Order order = Order.create(memberId, orderItems, paymentMethod);
        order = orderRepository.save(order);
        log.info("Order created with id={}, status={}", order.getId(), order.getStatus());

        // 4. Call Payment Service
        try {
            PaymentRequestDto paymentRequest = new PaymentRequestDto(
                    order.getId(),
                    order.getTotalAmount(),
                    paymentMethod.name());
            PaymentResponseDto paymentResponse = paymentClient.createPayment(paymentRequest);

            // 5. Update order status based on payment result
            if (paymentResponse.isCompleted()) {
                order.markAsConfirmed(paymentResponse.id());
                log.info("Order id={} confirmed, paymentId={}", order.getId(), paymentResponse.id());
            } else if (paymentResponse.isPending()) {
                order.markAsPending(paymentResponse.id());
                log.info("Order id={} pending payment, paymentId={}", order.getId(), paymentResponse.id());
            } else {
                order.markAsFailed();
                log.warn("Order id={} payment failed", order.getId());
            }
            order = orderRepository.save(order);

        } catch (PaymentFailedException e) {
            log.error("Payment failed for order id={}: {}", order.getId(), e.getMessage());
            order.markAsFailed();
            order = orderRepository.save(order);
            throw e;
        }

        return order;
    }

    private void validateMember(Long memberId) {
        log.debug("Validating member id={}", memberId);
        MemberDto member = memberClient.getMember(memberId)
                .orElseThrow(() -> new MemberValidationException(memberId, "Member does not exist"));

        if (!member.isActive()) {
            throw new MemberValidationException(memberId, "Member is not active (status: " + member.status() + ")");
        }
        log.debug("Member id={} validated successfully", memberId);
    }

    private List<OrderItem> validateAndBuildOrderItems(List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest request : itemRequests) {
            log.debug("Validating product id={}, quantity={}", request.productId(), request.quantity());

            // Get product info
            ProductDto product = productClient.getProduct(request.productId())
                    .orElseThrow(() -> new ProductValidationException(
                            request.productId(), "Product does not exist"));

            if (product.isDiscontinued()) {
                throw new ProductValidationException(request.productId(), "Product is discontinued");
            }
            if (!product.isAvailable()) {
                throw new ProductValidationException(request.productId(), "Product is not available");
            }

            // Check stock
            ProductStockDto stock = productClient.getStock(request.productId())
                    .orElseThrow(() -> new ProductValidationException(
                            request.productId(), "Cannot retrieve stock information"));

            if (!stock.hasAvailableStock(request.quantity())) {
                throw new ProductValidationException(
                        request.productId(),
                        String.format("Insufficient stock. Available: %d, Requested: %d",
                                stock.availableQuantity(), request.quantity()));
            }

            // Build OrderItem with product snapshot
            OrderItem item = new OrderItem(
                    product.id(),
                    product.name(),
                    product.price(),
                    request.quantity());
            orderItems.add(item);
            log.debug("Product id={} validated, subtotal={}", product.id(), item.getSubtotal());
        }

        return orderItems;
    }

    /**
     * Inner request class for order items.
     */
    public record OrderItemRequest(Long productId, int quantity) {
    }
}
