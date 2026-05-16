package com.amalitech.idempotency.service;

import com.amalitech.idempotency.config.IdempotencyProperties;
import com.amalitech.idempotency.store.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class IdempotencyKeySweeper {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeySweeper.class);

    private final IdempotencyStore store;
    private final IdempotencyProperties properties;

    public IdempotencyKeySweeper(IdempotencyStore store, IdempotencyProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    // fixedDelay (not fixedRate) so a slow sweep can never overlap itself.
    @Scheduled(fixedDelayString = "${idempotency.sweep-interval}")
    public void sweep() {
        Instant cutoff = Instant.now().minus(properties.ttl());
        int removed = store.sweepExpired(cutoff);
        if (removed > 0) {
            log.info("Swept {} expired idempotency entries", removed);
        }
    }
}
