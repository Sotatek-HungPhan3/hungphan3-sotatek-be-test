package com.sotatek.order.infrastructure.client;

import com.sotatek.order.application.dto.PaymentRequestDto;
import com.sotatek.order.application.dto.PaymentResponseDto;
import com.sotatek.order.application.exception.ExternalServiceException;
import com.sotatek.order.application.exception.PaymentFailedException;
import com.sotatek.order.application.port.out.PaymentClientPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP Client adapter for Payment Service.
 */
@Component
@ConditionalOnProperty(name = "external-services.mock", havingValue = "false")
public class PaymentClientAdapter implements PaymentClientPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentClientAdapter.class);

    private final RestClient restClient;

    public PaymentClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${external-services.payment.base-url}") String baseUrl,
            @Value("${external-services.payment.timeout:5000}") int timeout) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        log.info("Calling Payment Service for orderId={}, amount={}", request.orderId(), request.amount());
        try {
            PaymentResponseDto response = restClient.post()
                    .uri("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 422) {
                            throw new PaymentRejectedException("Payment rejected by payment service");
                        }
                        if (res.getStatusCode().value() == 400) {
                            throw new PaymentRejectedException("Invalid payment request");
                        }
                    })
                    .body(PaymentResponseDto.class);

            log.info("Payment Service response: status={}, transactionId={}",
                    response.status(), response.transactionId());
            return response;

        } catch (PaymentRejectedException e) {
            throw new PaymentFailedException(request.orderId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error calling Payment Service: {}", e.getMessage());
            throw new ExternalServiceException("PaymentService", "Failed to process payment: " + e.getMessage(), e);
        }
    }

    // Internal exception for flow control
    private static class PaymentRejectedException extends RuntimeException {
        PaymentRejectedException(String message) {
            super(message);
        }
    }
}
