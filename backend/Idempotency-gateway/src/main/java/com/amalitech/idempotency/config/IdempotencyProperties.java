package com.amalitech.idempotency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("idempotency")
public record IdempotencyProperties(Duration ttl, Duration sweepInterval) {
}
