package com.sotatek.order.application.port.out;

import com.sotatek.order.application.dto.PaymentRequestDto;
import com.sotatek.order.application.dto.PaymentResponseDto;

/**
 * Output port for Payment Service integration.
 */
public interface PaymentClientPort {

    /**
     * Create a payment.
     * 
     * @param request payment request
     * @return payment response with status
     * @throws com.sotatek.order.application.exception.PaymentFailedException   when
     *                                                                          payment
     *                                                                          fails
     * @throws com.sotatek.order.application.exception.ExternalServiceException on
     *                                                                          timeout/unavailable
     */
    PaymentResponseDto createPayment(PaymentRequestDto request);
}
