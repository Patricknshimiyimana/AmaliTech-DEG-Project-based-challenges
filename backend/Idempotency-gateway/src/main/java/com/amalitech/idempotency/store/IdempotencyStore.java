package com.amalitech.idempotency.store;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<CachedResponse> get(String key);

    void put(String key, CachedResponse entry);

    int sweepExpired(Instant cutoff);

    int size();
}
