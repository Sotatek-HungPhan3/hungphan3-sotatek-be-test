package com.sotatek.order.application.service;

import com.sotatek.order.application.port.out.OrderRepositoryPort;
import com.sotatek.order.domain.exception.OrderNotFoundException;
import com.sotatek.order.domain.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: Get Order by ID.
 */
@Service
@Transactional(readOnly = true)
public class GetOrderService {

    private static final Logger log = LoggerFactory.getLogger(GetOrderService.class);

    private final OrderRepositoryPort orderRepository;

    public GetOrderService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Get order by ID.
     *
     * @param orderId the order ID
     * @return the order
     * @throws OrderNotFoundException if order not found
     */
    public Order execute(Long orderId) {
        log.debug("Getting order id={}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
