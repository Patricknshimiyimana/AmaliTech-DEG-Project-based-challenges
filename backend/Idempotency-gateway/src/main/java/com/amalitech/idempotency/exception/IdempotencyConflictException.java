package com.amalitech.idempotency.exception;

public class IdempotencyConflictException extends RuntimeException {

    private static final String MESSAGE = "Idempotency key already used for a different request body.";

    private final String key;

    public IdempotencyConflictException(String key) {
        super(MESSAGE);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
