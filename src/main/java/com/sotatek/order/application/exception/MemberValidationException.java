package com.sotatek.order.application.exception;

/**
 * Exception when member validation fails.
 */
public class MemberValidationException extends RuntimeException {

    private final Long memberId;
    private final String reason;

    public MemberValidationException(Long memberId, String reason) {
        super(reason);
        this.memberId = memberId;
        this.reason = reason;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getReason() {
        return reason;
    }
}
