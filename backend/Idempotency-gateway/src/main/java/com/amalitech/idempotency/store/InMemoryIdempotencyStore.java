package com.amalitech.idempotency.store;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentMap<String, StoreEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<StoreEntry> get(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public Optional<StoreEntry> putIfAbsent(String key, StoreEntry entry) {
        return Optional.ofNullable(entries.putIfAbsent(key, entry));
    }

    @Override
    public void put(String key, StoreEntry entry) {
        entries.put(key, entry);
    }

    @Override
    public void remove(String key) {
        entries.remove(key);
    }

    @Override
    public int sweepExpired(Instant cutoff) {
        int before = entries.size();
        entries.entrySet().removeIf(e -> createdAt(e.getValue()).isBefore(cutoff));
        return before - entries.size();
    }

    @Override
    public int size() {
        return entries.size();
    }

    private static Instant createdAt(StoreEntry entry) {
        if (entry instanceof CachedResponse done)
            return done.createdAt();
        if (entry instanceof Pending pending)
            return pending.createdAt();
        throw new IllegalStateException("Unknown StoreEntry: " + entry);
    }
}
