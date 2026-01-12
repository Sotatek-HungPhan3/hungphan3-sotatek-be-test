package com.sotatek.order.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for Order.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    Page<OrderEntity> findByMemberId(Long memberId, Pageable pageable);
}
