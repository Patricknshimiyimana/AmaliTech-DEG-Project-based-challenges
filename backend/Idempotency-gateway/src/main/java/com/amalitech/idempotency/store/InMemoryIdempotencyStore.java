package com.amalitech.idempotency.store;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentMap<String, CachedResponse> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<CachedResponse> get(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void put(String key, CachedResponse entry) {
        entries.put(key, entry);
    }

    @Override
    public int sweepExpired(Instant cutoff) {
        int before = entries.size();
        entries.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
        return before - entries.size();
    }

    @Override
    public int size() {
        return entries.size();
    }
}
