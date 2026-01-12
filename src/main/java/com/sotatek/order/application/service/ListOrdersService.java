package com.sotatek.order.application.service;

import com.sotatek.order.application.port.out.OrderRepositoryPort;
import com.sotatek.order.domain.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: List Orders with pagination.
 */
@Service
@Transactional(readOnly = true)
public class ListOrdersService {

    private static final Logger log = LoggerFactory.getLogger(ListOrdersService.class);

    private final OrderRepositoryPort orderRepository;

    public ListOrdersService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * List all orders with pagination.
     */
    public Page<Order> execute(Pageable pageable) {
        log.debug("Listing orders, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAll(pageable);
    }

    /**
     * List orders by member ID with pagination.
     */
    public Page<Order> executeByMember(Long memberId, Pageable pageable) {
        log.debug("Listing orders for memberId={}, page={}, size={}",
                memberId, pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findByMemberId(memberId, pageable);
    }
}
