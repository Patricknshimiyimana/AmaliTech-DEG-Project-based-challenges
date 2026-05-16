package com.amalitech.idempotency.service;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.store.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyServiceConcurrencyTest {

    @Test
    void concurrentIdenticalRequestsShareSingleProcessing() throws Exception {
        AtomicInteger processCount = new AtomicInteger();
        CountDownLatch processingStarted = new CountDownLatch(1);
        CountDownLatch releaseProcessing = new CountDownLatch(1);

        // Test double: a processor that blocks until we let it finish, so we can
        // deterministically arrange "B arrives while A is still processing".
        PaymentProcessor slow = new PaymentProcessor() {
            @Override
            public PaymentResponse process(PaymentRequest request) {
                processCount.incrementAndGet();
                processingStarted.countDown();
                try {
                    assertTrue(releaseProcessing.await(5, TimeUnit.SECONDS),
                            "Test timed out waiting to release processing");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return new PaymentResponse("Charged %s %s"
                        .formatted(request.amount().toPlainString(), request.currency()));
            }
        };

        IdempotencyService service = new IdempotencyService(
                new InMemoryIdempotencyStore(), slow);

        PaymentRequest req = new PaymentRequest(new BigDecimal("100"), "GHS");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // Request A — should claim the slot and start processing.
            Future<IdempotencyResult> a = pool.submit(() -> service.handle("k1", req));

            // Wait until A has entered the processor before launching B.
            assertTrue(processingStarted.await(2, TimeUnit.SECONDS),
                    "Request A never entered the processor");

            // Request B — should see Pending and await A's future.
            Future<IdempotencyResult> b = pool.submit(() -> service.handle("k1", req));

            // Give B a moment to enter the wait. (Small sleep is acceptable here;
            // the assertion below proves we never invoked the processor twice.)
            Thread.sleep(100);

            // Let A finish.
            releaseProcessing.countDown();

            IdempotencyResult ra = a.get(2, TimeUnit.SECONDS);
            IdempotencyResult rb = b.get(2, TimeUnit.SECONDS);

            assertEquals(1, processCount.get(),
                    "PaymentProcessor must be invoked exactly once for two concurrent identical requests");
            assertEquals(ra.entry(), rb.entry(),
                    "Both requests must receive the same cached response");
            assertFalse(ra.cacheHit(), "Winner of the race is not a cache hit");
            assertTrue(rb.cacheHit(), "Loser of the race sees a cache hit");
        } finally {
            pool.shutdownNow();
        }
    }
}
