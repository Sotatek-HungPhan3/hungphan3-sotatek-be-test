package com.sotatek.order.infrastructure.client.mock;

import com.sotatek.order.application.dto.MemberDto;
import com.sotatek.order.application.port.out.MemberClientPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mock implementation of MemberClientPort.
 * Activated when: external-services.mock=true
 */
@Component
@ConditionalOnProperty(name = "external-services.mock", havingValue = "true")
public class MockMemberClientAdapter implements MemberClientPort {

    private static final Logger log = LoggerFactory.getLogger(MockMemberClientAdapter.class);

    @Override
    public Optional<MemberDto> getMember(Long memberId) {
        log.info("[MOCK] Getting member id={}", memberId);

        // Simulate different scenarios based on memberId
        return switch (memberId.intValue()) {
            case 999 -> {
                log.info("[MOCK] Member 999 not found");
                yield Optional.empty();
            }
            case 998 -> {
                log.info("[MOCK] Member 998 is INACTIVE");
                yield Optional.of(new MemberDto(998L, "Inactive User", "inactive@test.com", "INACTIVE", "BRONZE"));
            }
            case 997 -> {
                log.info("[MOCK] Member 997 is SUSPENDED");
                yield Optional.of(new MemberDto(997L, "Suspended User", "suspended@test.com", "SUSPENDED", "BRONZE"));
            }
            default -> {
                log.info("[MOCK] Member {} is ACTIVE", memberId);
                yield Optional.of(new MemberDto(
                        memberId,
                        "Mock User " + memberId,
                        "user" + memberId + "@test.com",
                        "ACTIVE",
                        "GOLD"));
            }
        };
    }
}
