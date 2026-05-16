package com.amalitech.idempotency.service;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.store.CachedResponse;
import com.amalitech.idempotency.store.IdempotencyStore;
import com.amalitech.idempotency.util.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IdempotencyService {

    private final IdempotencyStore store;
    private final PaymentProcessor processor;

    public IdempotencyService(IdempotencyStore store, PaymentProcessor processor) {
        this.store = store;
        this.processor = processor;
    }

    public CachedResponse handle(String key, PaymentRequest body) {
        String bodyHash = HashUtil.sha256Canonical(body);
        PaymentResponse response = processor.process(body);
        CachedResponse entry = new CachedResponse(
                HttpStatus.OK.value(),
                response,
                bodyHash,
                Instant.now());
        store.put(key, entry);
        return entry;
    }
}
