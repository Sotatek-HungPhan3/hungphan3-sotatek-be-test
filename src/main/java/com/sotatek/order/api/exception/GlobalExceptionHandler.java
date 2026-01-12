package com.sotatek.order.api.exception;

import com.sotatek.order.api.dto.ErrorResponse;
import com.sotatek.order.application.exception.*;
import com.sotatek.order.domain.exception.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * Maps domain/application exceptions to appropriate HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("ORDER_NOT_FOUND", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex,
            WebRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_STATE_TRANSITION", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex, WebRequest request) {
        log.warn("Domain logic error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("DOMAIN_ERROR", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(MemberValidationException.class)
    public ResponseEntity<ErrorResponse> handleMemberValidation(MemberValidationException ex, WebRequest request) {
        log.warn("Member validation failed: {}", ex.getMessage());
        HttpStatus status = ex.getReason().contains("does not exist") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        String code = ex.getReason().contains("does not exist") ? "MEMBER_NOT_FOUND" : "MEMBER_NOT_ACTIVE";
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(code, ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(ProductValidationException.class)
    public ResponseEntity<ErrorResponse> handleProductValidation(ProductValidationException ex, WebRequest request) {
        log.warn("Product validation failed: {}", ex.getMessage());
        HttpStatus status = ex.getReason().contains("does not exist") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        String code;
        if (ex.getReason().contains("does not exist")) {
            code = "PRODUCT_NOT_FOUND";
        } else if (ex.getReason().contains("discontinued")) {
            code = "PRODUCT_DISCONTINUED";
        } else if (ex.getReason().contains("Insufficient")) {
            code = "INSUFFICIENT_STOCK";
        } else {
            code = "PRODUCT_NOT_AVAILABLE";
        }
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(code, ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex, WebRequest request) {
        log.warn("Payment failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("PAYMENT_FAILED", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex, WebRequest request) {
        log.error("External service error: {} - {}", ex.getServiceName(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("SERVICE_UNAVAILABLE",
                        "External service temporarily unavailable: " + ex.getServiceName(),
                        getPath(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", errors, getPath(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex,
            WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_REQUEST_FORMAT", "Malformed JSON request or missing body",
                        getPath(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_REQUEST", ex.getMessage(), getPath(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", getPath(request)));
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
