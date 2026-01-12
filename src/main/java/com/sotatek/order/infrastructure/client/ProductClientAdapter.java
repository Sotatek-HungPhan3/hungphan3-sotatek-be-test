package com.sotatek.order.infrastructure.client;

import com.sotatek.order.application.dto.ProductDto;
import com.sotatek.order.application.dto.ProductStockDto;
import com.sotatek.order.application.exception.ExternalServiceException;
import com.sotatek.order.application.port.out.ProductClientPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * HTTP Client adapter for Product Service.
 */
@Component
public class ProductClientAdapter implements ProductClientPort {

    private static final Logger log = LoggerFactory.getLogger(ProductClientAdapter.class);

    private final RestClient restClient;

    public ProductClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${external-services.product.base-url}") String baseUrl,
            @Value("${external-services.product.timeout:5000}") int timeout) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Optional<ProductDto> getProduct(Long productId) {
        log.debug("Calling Product Service to get product id={}", productId);
        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ProductNotFoundException();
                        }
                    })
                    .body(ProductDto.class);

            log.debug("Product Service returned: {}", product);
            return Optional.ofNullable(product);

        } catch (ProductNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error calling Product Service: {}", e.getMessage());
            throw new ExternalServiceException("ProductService", "Failed to get product: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ProductStockDto> getStock(Long productId) {
        log.debug("Calling Product Service to get stock for product id={}", productId);
        try {
            ProductStockDto stock = restClient.get()
                    .uri("/api/products/{productId}/stock", productId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ProductNotFoundException();
                        }
                    })
                    .body(ProductStockDto.class);

            log.debug("Product Service returned stock: {}", stock);
            return Optional.ofNullable(stock);

        } catch (ProductNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error calling Product Service for stock: {}", e.getMessage());
            throw new ExternalServiceException("ProductService", "Failed to get stock: " + e.getMessage(), e);
        }
    }

    // Internal exception for flow control
    private static class ProductNotFoundException extends RuntimeException {
    }
}
