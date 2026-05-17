package com.amalitech.idempotency.service;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.exception.IdempotencyConflictException;
import com.amalitech.idempotency.store.CachedResponse;
import com.amalitech.idempotency.store.IdempotencyStore;
import com.amalitech.idempotency.store.Pending;
import com.amalitech.idempotency.store.StoreEntry;
import com.amalitech.idempotency.util.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

        CompletableFuture<CachedResponse> myFuture = new CompletableFuture<>();
        Pending myPending = new Pending(requestHash, myFuture, Instant.now());

        Optional<StoreEntry> existing = store.putIfAbsent(key, myPending);

        if (existing.isEmpty()) {
            return processAndComplete(key, body, requestHash, myFuture);
        }

        StoreEntry slot = existing.get();

        if (!slot.bodyHash().equals(requestHash)) {
            throw new IdempotencyConflictException(key);
        }

        if (slot instanceof CachedResponse done) {
            return new IdempotencyResult(done, true);
        }
        if (slot instanceof Pending pending) {
            return awaitInFlight(pending);
        }
        throw new IllegalStateException("Unknown StoreEntry: " + slot);
    }

    private IdempotencyResult processAndComplete(
            String key,
            PaymentRequest body,
            String requestHash,
            CompletableFuture<CachedResponse> future) {
        try {
            PaymentResponse response = processor.process(body);
            CachedResponse completed = new CachedResponse(
                    HttpStatus.OK.value(),
                    response,
                    requestHash,
                    Instant.now());
            store.put(key, completed);
            future.complete(completed);
            return new IdempotencyResult(completed, false);
        } catch (RuntimeException e) {
            // Free the slot so retries can be processed; unblock any waiters.
            store.remove(key);
            future.completeExceptionally(e);
            throw e;
        }
    }

    private IdempotencyResult awaitInFlight(Pending pending) {
        try {
            CachedResponse completed = pending.future().get();
            return new IdempotencyResult(completed, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting in-flight payment", e);
        } catch (ExecutionException e) {
            // Original processing failed; propagate the underlying cause.
            if (e.getCause() instanceof RuntimeException rte) {
                throw rte;
            }
            throw new IllegalStateException("In-flight payment failed", e.getCause());
        }
    }
}
