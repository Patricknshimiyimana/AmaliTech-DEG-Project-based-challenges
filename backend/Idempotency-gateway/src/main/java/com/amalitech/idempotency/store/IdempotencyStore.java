package com.amalitech.idempotency.store;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<StoreEntry> get(String key);

    Optional<StoreEntry> putIfAbsent(String key, StoreEntry entry);

    void put(String key, StoreEntry entry);

    void remove(String key);

    int sweepExpired(Instant cutoff);

    int size();
}
