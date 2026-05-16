package com.amalitech.idempotency.store;

import com.amalitech.idempotency.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryIdempotencyStoreTest {

    @Test
    void putIfAbsent_returnsEmpty_whenKeyIsFree() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        Pending pending = new Pending("hash", new CompletableFuture<>(), Instant.now());

        Optional<StoreEntry> result = store.putIfAbsent("k", pending);

        assertTrue(result.isEmpty(), "First insert returns empty");
        assertEquals(1, store.size());
        assertSame(pending, store.get("k").orElseThrow());
    }

    @Test
    void putIfAbsent_returnsExisting_andDoesNotOverwrite() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        Pending first = new Pending("hash", new CompletableFuture<>(), Instant.now());
        Pending second = new Pending("hash", new CompletableFuture<>(), Instant.now());

        store.putIfAbsent("k", first);
        Optional<StoreEntry> result = store.putIfAbsent("k", second);

        assertTrue(result.isPresent(), "Second insert returns the existing entry");
        assertSame(first, result.get(), "Existing entry is what's returned");
        assertSame(first, store.get("k").orElseThrow(), "Store still holds the first entry");
    }

    @Test
    void sweepExpired_removesOldPendingAndCachedEntries_keepsFresh() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        Instant old = Instant.now().minus(Duration.ofHours(25));
        Instant fresh = Instant.now();

        store.put("old-pending", new Pending("h1", new CompletableFuture<>(), old));
        store.put("old-cached", new CachedResponse(
                HttpStatus.OK.value(), new PaymentResponse("ok"), "h2", old));
        store.put("fresh-cached", new CachedResponse(
                HttpStatus.OK.value(), new PaymentResponse("ok"), "h3", fresh));

        int removed = store.sweepExpired(Instant.now().minus(Duration.ofHours(24)));

        assertEquals(2, removed, "Both 25h-old entries removed regardless of Pending vs CachedResponse");
        assertEquals(1, store.size());
        assertTrue(store.get("fresh-cached").isPresent());
    }
}
