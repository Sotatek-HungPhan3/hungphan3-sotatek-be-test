package com.sotatek.order.domain.exception;

/**
 * Thrown when an invalid state transition is attempted.
 * E.g., canceling a CONFIRMED order.
 */
public class InvalidStateTransitionException extends DomainException {

    private final String fromState;
    private final String toState;

    public InvalidStateTransitionException(String message, String fromState, String toState) {
        super(message);
        this.fromState = fromState;
        this.toState = toState;
    }

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }
}
