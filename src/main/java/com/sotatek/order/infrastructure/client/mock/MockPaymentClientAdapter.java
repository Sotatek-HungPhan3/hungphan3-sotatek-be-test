package com.sotatek.order.infrastructure.client.mock;

import com.sotatek.order.application.dto.PaymentRequestDto;
import com.sotatek.order.application.dto.PaymentResponseDto;
import com.sotatek.order.application.exception.PaymentFailedException;
import com.sotatek.order.application.port.out.PaymentClientPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mock implementation of PaymentClientPort.
 * Activated when: external-services.mock=true
 */
@Component
@ConditionalOnProperty(name = "external-services.mock", havingValue = "true")
public class MockPaymentClientAdapter implements PaymentClientPort {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentClientAdapter.class);

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        log.info("[MOCK] Creating payment for orderId={}, amount={}, method={}",
                request.orderId(), request.amount(), request.paymentMethod());

        // Simulate different payment scenarios based on amount
        BigDecimal amount = request.amount();

        // Amount ending with .99 -> FAILED
        if (amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.valueOf(0.99)) == 0) {
            log.warn("[MOCK] Payment FAILED for amount={}", amount);
            throw new PaymentFailedException(request.orderId(), "Insufficient funds (mock)");
        }

        // Amount ending with .50 -> PENDING
        if (amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.valueOf(0.50)) == 0) {
            log.info("[MOCK] Payment PENDING for amount={}", amount);
            return new PaymentResponseDto(
                    System.currentTimeMillis(),
                    request.orderId(),
                    request.amount(),
                    "PENDING",
                    "TXN-MOCK-PENDING-" + System.currentTimeMillis(),
                    LocalDateTime.now());
        }

        // Default -> COMPLETED
        log.info("[MOCK] Payment COMPLETED for amount={}", amount);
        return new PaymentResponseDto(
                System.currentTimeMillis(),
                request.orderId(),
                request.amount(),
                "COMPLETED",
                "TXN-MOCK-SUCCESS-" + System.currentTimeMillis(),
                LocalDateTime.now());
    }
}
