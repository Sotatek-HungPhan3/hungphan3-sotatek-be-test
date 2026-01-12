package com.sotatek.order.infrastructure.client.mock;

import com.sotatek.order.application.dto.ProductDto;
import com.sotatek.order.application.dto.ProductStockDto;
import com.sotatek.order.application.port.out.ProductClientPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Mock implementation of ProductClientPort.
 * Activated when: external-services.mock=true
 */
@Component
@ConditionalOnProperty(name = "external-services.mock", havingValue = "true")
public class MockProductClientAdapter implements ProductClientPort {

    private static final Logger log = LoggerFactory.getLogger(MockProductClientAdapter.class);

    @Override
    public Optional<ProductDto> getProduct(Long productId) {
        log.info("[MOCK] Getting product id={}", productId);

        return switch (productId.intValue()) {
            case 999 -> {
                log.info("[MOCK] Product 999 not found");
                yield Optional.empty();
            }
            case 998 -> {
                log.info("[MOCK] Product 998 is DISCONTINUED");
                yield Optional
                        .of(new ProductDto(998L, "Discontinued Product", BigDecimal.valueOf(99.99), "DISCONTINUED"));
            }
            case 997 -> {
                log.info("[MOCK] Product 997 is OUT_OF_STOCK");
                yield Optional
                        .of(new ProductDto(997L, "Out of Stock Product", BigDecimal.valueOf(49.99), "OUT_OF_STOCK"));
            }
            default -> {
                log.info("[MOCK] Product {} is AVAILABLE", productId);
                yield Optional.of(new ProductDto(
                        productId,
                        "Mock Product " + productId,
                        BigDecimal.valueOf(29.99),
                        "AVAILABLE"));
            }
        };
    }

    @Override
    public Optional<ProductStockDto> getStock(Long productId) {
        log.info("[MOCK] Getting stock for product id={}", productId);

        return switch (productId.intValue()) {
            case 999 -> {
                log.info("[MOCK] Stock for product 999 not found");
                yield Optional.empty();
            }
            case 996 -> {
                log.info("[MOCK] Product 996 has insufficient stock (only 2 available)");
                yield Optional.of(new ProductStockDto(996L, 10, 8, 2));
            }
            default -> {
                log.info("[MOCK] Product {} has plenty of stock", productId);
                yield Optional.of(new ProductStockDto(
                        productId,
                        1000, // total quantity
                        50, // reserved
                        950 // available
                ));
            }
        };
    }
}
