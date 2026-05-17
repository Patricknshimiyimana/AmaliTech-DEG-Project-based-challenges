package com.amalitech.idempotency.service;

import com.amalitech.idempotency.config.IdempotencyProperties;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.store.CachedResponse;
import com.amalitech.idempotency.store.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyKeySweeperTest {

    private static final IdempotencyProperties PROPS =
            new IdempotencyProperties(Duration.ofHours(24), Duration.ofHours(1));

    @Test
    void removesEntriesOlderThanTtl() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        IdempotencyKeySweeper sweeper = new IdempotencyKeySweeper(store, PROPS);

        Instant now = Instant.now();
        store.put("expired", cached("hash-1", now.minus(Duration.ofHours(25))));
        store.put("fresh", cached("hash-2", now.minus(Duration.ofHours(1))));

        sweeper.sweep();

        assertEquals(1, store.size(), "Only the expired entry should be removed");
        assertTrue(store.get("fresh").isPresent(), "Fresh entry should remain");
        assertFalse(store.get("expired").isPresent(), "Expired entry should be gone");
    }

    @Test
    void leavesAllEntriesWhenNoneExpired() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        IdempotencyKeySweeper sweeper = new IdempotencyKeySweeper(store, PROPS);

        Instant now = Instant.now();
        store.put("a", cached("h-a", now));
        store.put("b", cached("h-b", now.minus(Duration.ofHours(23))));

        sweeper.sweep();

        assertEquals(2, store.size(), "No entries should be removed when all are within TTL");
    }

    private static CachedResponse cached(String bodyHash, Instant createdAt) {
        return new CachedResponse(
                HttpStatus.OK.value(),
                new PaymentResponse("Charged 100 GHS"),
                bodyHash,
                createdAt
        );
    }
}
