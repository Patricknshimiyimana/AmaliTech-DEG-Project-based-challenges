package com.amalitech.idempotency.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashUtilTest {

    @Test
    void sameContentInDifferentOrderProducesSameHash() {
        // LinkedHashMap preserves insertion order so we can prove the
        // canonical mapper is what normalises the order.
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("amount", 100);
        ordered.put("currency", "GHS");

        Map<String, Object> reversed = new LinkedHashMap<>();
        reversed.put("currency", "GHS");
        reversed.put("amount", 100);

        assertEquals(
                HashUtil.sha256Canonical(ordered),
                HashUtil.sha256Canonical(reversed),
                "Semantically identical bodies must hash identically"
        );
    }

    @Test
    void differentContentProducesDifferentHash() {
        assertNotEquals(
                HashUtil.sha256Canonical(Map.of("amount", 100)),
                HashUtil.sha256Canonical(Map.of("amount", 500))
        );
    }

    @Test
    void hashIsStableLowercaseHex64Chars() {
        String hash = HashUtil.sha256Canonical(Map.of("k", "v"));
        assertEquals(64, hash.length(), "SHA-256 hex is 64 chars");
        assertTrue(hash.matches("[0-9a-f]{64}"), "Hex must be lowercase 0-9a-f");
    }
}
