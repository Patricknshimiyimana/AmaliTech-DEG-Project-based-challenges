package com.amalitech.idempotency.service;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.exception.IdempotencyConflictException;
import com.amalitech.idempotency.store.CachedResponse;
import com.amalitech.idempotency.store.IdempotencyStore;
import com.amalitech.idempotency.util.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyStore store;
    private final PaymentProcessor processor;

    public IdempotencyService(IdempotencyStore store, PaymentProcessor processor) {
        this.store = store;
        this.processor = processor;
    }

    public IdempotencyResult handle(String key, PaymentRequest body) {
        String requestHash = HashUtil.sha256Canonical(body);

        Optional<CachedResponse> existing = store.get(key);
        if (existing.isPresent()) {
            CachedResponse cached = existing.get();
            if (!cached.bodyHash().equals(requestHash)) {
                throw new IdempotencyConflictException(key);
            }
            return new IdempotencyResult(cached, true);
        }

        PaymentResponse response = processor.process(body);
        CachedResponse entry = new CachedResponse(
                HttpStatus.OK.value(),
                response,
                requestHash,
                Instant.now());
        store.put(key, entry);
        return new IdempotencyResult(entry, false);
    }
}
