package com.sotatek.order.api.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sotatek.order.api.dto.CancelOrderRequest;
import com.sotatek.order.api.dto.CreateOrderRequest;
import com.sotatek.order.api.dto.OrderItemRequest;
import com.sotatek.order.api.dto.OrderResponse;
import com.sotatek.order.domain.model.OrderStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0); // Random port
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("external-services.member.base-url", wireMockServer::baseUrl);
        registry.add("external-services.product.base-url", wireMockServer::baseUrl);
        registry.add("external-services.payment.base-url", wireMockServer::baseUrl);
        registry.add("external-services.mock", () -> "false");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @Test
    void createOrder_Success_ShouldReturnConfirmedOrder() {
        stubMember(1L, "ACTIVE");
        stubProduct(101L, "AVAILABLE", 100);
        stubPayment(200.00, "COMPLETED");

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemRequest(101L, 2)), "CREDIT_CARD");

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity("/api/orders", request,
                OrderResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(OrderStatus.CONFIRMED, response.getBody().status());
    }

    @Test
    void createOrder_ValidationFails_ShouldReturn400() {
        // Missing items
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(), "CREDIT_CARD");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/orders", request, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getOrder_NotFound_ShouldReturn404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/orders/9999", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listOrders_ShouldReturnPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/orders", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("content"));
    }

    @Test
    void createOrder_MemberServiceTimeout_ShouldReturn503() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo("/api/members/1"))
                .willReturn(aResponse()
                        .withFixedDelay(6000) // Longer than configured timeout (5000)
                        .withHeader("Content-Type", "application/json")));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemRequest(101L, 1)), "CREDIT_CARD");

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity("/api/orders", request, String.class);

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("SERVICE_UNAVAILABLE"));
    }

    @Test
    void createOrder_ProductServiceError_ShouldReturn503() {
        stubMember(1L, "ACTIVE");
        wireMockServer.stubFor(get(urlEqualTo("/api/products/101"))
                .willReturn(aResponse().withStatus(500)));

        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new OrderItemRequest(101L, 1)), "CREDIT_CARD");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/orders", request, String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    // Helper stubs
    private void stubMember(Long id, String status) {
        wireMockServer.stubFor(get(urlEqualTo("/api/members/" + id))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format(
                                "{\"id\": %d, \"name\": \"User\", \"email\": \"u@e.com\", \"status\": \"%s\", \"grade\": \"GOLD\"}",
                                id, status))));
    }

    private void stubProduct(Long id, String status, int stock) {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/" + id))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format(
                                "{\"id\": %d, \"name\": \"Product\", \"price\": 50.00, \"status\": \"%s\"}", id,
                                status))));
        wireMockServer.stubFor(get(urlEqualTo("/api/products/" + id + "/stock"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format(
                                "{\"productId\": %d, \"totalQuantity\": %d, \"reservedQuantity\": 0, \"availableQuantity\": %d}",
                                id, stock, stock))));
    }

    private void stubPayment(double amount, String status) {
        wireMockServer.stubFor(post(urlEqualTo("/api/payments"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format(
                                "{\"id\": 999, \"orderId\": 1, \"amount\": %.2f, \"status\": \"%s\", \"transactionId\": \"TXN-1\", \"timestamp\": \"2023-10-01T10:00:00\"}",
                                amount, status))));
    }
}
