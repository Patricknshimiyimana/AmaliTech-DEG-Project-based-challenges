package com.amalitech.idempotency.store;

// A slot in the idempotency store is either still processing (Pending)
// or has a completed response cached (CachedResponse).
public sealed interface StoreEntry permits Pending, CachedResponse {

    String bodyHash();
}
