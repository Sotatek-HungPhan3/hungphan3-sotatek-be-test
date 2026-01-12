package com.sotatek.order.application.service;

import com.sotatek.order.application.port.out.OrderRepositoryPort;
import com.sotatek.order.domain.exception.OrderNotFoundException;
import com.sotatek.order.domain.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: Cancel Order.
 * Only PENDING and FAILED orders can be cancelled.
 * CONFIRMED orders cannot be cancelled (already paid).
 */
@Service
@Transactional
public class CancelOrderService {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderService.class);

    private final OrderRepositoryPort orderRepository;

    public CancelOrderService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Cancel an order.
     *
     * @param orderId the order ID to cancel
     * @return the cancelled order
     * @throws OrderNotFoundException                                             if
     *                                                                            order
     *                                                                            not
     *                                                                            found
     * @throws com.sotatek.order.domain.exception.InvalidStateTransitionException if
     *                                                                            order
     *                                                                            cannot
     *                                                                            be
     *                                                                            cancelled
     */
    public Order execute(Long orderId) {
        log.info("Cancelling order id={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Domain logic handles state transition validation
        order.cancel();
        order = orderRepository.save(order);

        log.info("Order id={} cancelled successfully", orderId);
        return order;
    }
}
