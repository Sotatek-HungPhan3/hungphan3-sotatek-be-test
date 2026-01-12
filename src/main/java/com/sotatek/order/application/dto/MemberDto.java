package com.sotatek.order.application.dto;

/**
 * Member DTO from external Member Service.
 */
public record MemberDto(
        Long id,
        String name,
        String email,
        String status,
        String grade) {
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
