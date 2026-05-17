package com.amalitech.idempotency.service;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessor {

    // Per User Story 1: simulate processing with a 2-second delay.
    // this is where the call to Stripe / PayPal / bank gateway would go.
    private static final long SIMULATED_DELAY_MS = 2_000L;

    public PaymentResponse process(PaymentRequest request) {
        try {
            Thread.sleep(SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment processing interrupted", e);
        }
        return new PaymentResponse(
                "Charged %s %s".formatted(request.amount().toPlainString(), request.currency()));
    }
}
