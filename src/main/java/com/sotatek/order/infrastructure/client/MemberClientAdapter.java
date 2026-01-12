package com.sotatek.order.infrastructure.client;

import com.sotatek.order.application.dto.MemberDto;
import com.sotatek.order.application.exception.ExternalServiceException;
import com.sotatek.order.application.port.out.MemberClientPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

/**
 * HTTP Client adapter for Member Service.
 * Activated when: external-services.mock=false
 */
@Component
@ConditionalOnProperty(name = "external-services.mock", havingValue = "false")
public class MemberClientAdapter implements MemberClientPort {

    private static final Logger log = LoggerFactory.getLogger(MemberClientAdapter.class);

    private final RestClient restClient;

    public MemberClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${external-services.member.base-url}") String baseUrl,
            @Value("${external-services.member.timeout:5000}") int timeout) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Optional<MemberDto> getMember(Long memberId) {
        log.debug("Calling Member Service to get member id={}", memberId);
        try {
            MemberDto member = restClient.get()
                    .uri("/api/members/{memberId}", memberId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            // Return empty optional for not found
                            throw new MemberNotFoundException();
                        }
                    })
                    .body(MemberDto.class);

            log.debug("Member Service returned: {}", member);
            return Optional.ofNullable(member);

        } catch (MemberNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error calling Member Service: {}", e.getMessage());
            throw new ExternalServiceException("MemberService", "Failed to get member: " + e.getMessage(), e);
        }
    }

    // Internal exception for flow control
    private static class MemberNotFoundException extends RuntimeException {
    }
}
