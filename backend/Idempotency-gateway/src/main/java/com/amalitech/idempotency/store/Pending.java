package com.amalitech.idempotency.store;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

// A request that has claimed the idempotency slot and is currently being processed.
// Concurrent identical requests await the same `future` to deduplicate work.
public record Pending(
        String bodyHash,
        CompletableFuture<CachedResponse> future,
        Instant createdAt
) implements StoreEntry {
}
