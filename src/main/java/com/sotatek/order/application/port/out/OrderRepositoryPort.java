package com.sotatek.order.application.port.out;

import com.sotatek.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Output port for Order persistence.
 */
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Page<Order> findAll(Pageable pageable);

    Page<Order> findByMemberId(Long memberId, Pageable pageable);
}
