package com.sotatek.order.infrastructure.persistence;

import com.sotatek.order.application.port.out.OrderRepositoryPort;
import com.sotatek.order.domain.model.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter implementing OrderRepositoryPort using JPA.
 */
@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderMapper.toEntity(order);
        OrderEntity savedEntity = jpaRepository.save(entity);
        return OrderMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id)
                .map(OrderMapper::toDomain);
    }

    @Override
    public Page<Order> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(OrderMapper::toDomain);
    }

    @Override
    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return jpaRepository.findByMemberId(memberId, pageable)
                .map(OrderMapper::toDomain);
    }
}
