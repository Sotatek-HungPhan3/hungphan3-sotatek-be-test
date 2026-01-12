package com.sotatek.order.api.controller;

import com.sotatek.order.api.dto.*;
import com.sotatek.order.application.service.*;
import com.sotatek.order.domain.model.Order;
import com.sotatek.order.domain.model.PaymentMethod;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Order operations.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final CreateOrderService createOrderService;
    private final GetOrderService getOrderService;
    private final ListOrdersService listOrdersService;
    private final CancelOrderService cancelOrderService;

    public OrderController(
            CreateOrderService createOrderService,
            GetOrderService getOrderService,
            ListOrdersService listOrdersService,
            CancelOrderService cancelOrderService) {
        this.createOrderService = createOrderService;
        this.getOrderService = getOrderService;
        this.listOrdersService = listOrdersService;
        this.cancelOrderService = cancelOrderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates an order, validates member/products, and processes payment")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/orders - memberId={}", request.memberId());

        // Convert request items
        List<CreateOrderService.OrderItemRequest> items = request.items().stream()
                .map(item -> new CreateOrderService.OrderItemRequest(item.productId(), item.quantity()))
                .toList();

        // Parse payment method
        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.paymentMethod().toUpperCase());

        // Execute use case
        Order order = createOrderService.execute(request.memberId(), items, paymentMethod);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(OrderResponse.fromDomain(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details by order ID")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        log.info("GET /api/orders/{}", id);
        Order order = getOrderService.execute(id);
        return ResponseEntity.ok(OrderResponse.fromDomain(order));
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Lists orders with pagination")
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @Parameter(description = "Filter by member ID") @RequestParam(required = false) Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /api/orders - memberId={}, page={}", memberId, pageable.getPageNumber());

        Page<Order> orders;
        if (memberId != null) {
            orders = listOrdersService.executeByMember(memberId, pageable);
        } else {
            orders = listOrdersService.execute(pageable);
        }

        return ResponseEntity.ok(orders.map(OrderResponse::fromDomain));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancels an order (only PENDING/FAILED orders can be cancelled)")
    public ResponseEntity<OrderResponse> updateOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request) {
        log.info("PUT /api/orders/{} - status={}", id, request.status());

        if (!request.isCancelRequest()) {
            throw new IllegalArgumentException("Only CANCELLED status is supported for update");
        }

        Order order = cancelOrderService.execute(id);
        return ResponseEntity.ok(OrderResponse.fromDomain(order));
    }
}
