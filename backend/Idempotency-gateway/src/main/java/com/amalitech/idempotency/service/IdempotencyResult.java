package com.amalitech.idempotency.service;

import com.amalitech.idempotency.store.CachedResponse;

public record IdempotencyResult(CachedResponse entry, boolean cacheHit) {
}
