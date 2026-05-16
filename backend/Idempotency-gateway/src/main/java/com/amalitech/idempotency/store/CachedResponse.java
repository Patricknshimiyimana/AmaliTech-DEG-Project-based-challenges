package com.amalitech.idempotency.store;

import com.amalitech.idempotency.dto.PaymentResponse;

import java.time.Instant;

public record CachedResponse(
        int statusCode,
        PaymentResponse body,
        String bodyHash,
        Instant createdAt
) {
}
