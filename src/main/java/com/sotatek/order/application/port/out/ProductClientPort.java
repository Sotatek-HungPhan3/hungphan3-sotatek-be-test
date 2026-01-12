package com.sotatek.order.application.port.out;

import com.sotatek.order.application.dto.ProductDto;
import com.sotatek.order.application.dto.ProductStockDto;

import java.util.Optional;

/**
 * Output port for Product Service integration.
 */
public interface ProductClientPort {

    /**
     * Get product by ID.
     */
    Optional<ProductDto> getProduct(Long productId);

    /**
     * Get product stock info.
     */
    Optional<ProductStockDto> getStock(Long productId);
}
