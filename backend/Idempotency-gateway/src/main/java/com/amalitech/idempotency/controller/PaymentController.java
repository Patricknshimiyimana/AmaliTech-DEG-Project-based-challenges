package com.amalitech.idempotency.controller;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.service.IdempotencyResult;
import com.amalitech.idempotency.service.IdempotencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated // required for @NotBlank to apply
public class PaymentController {

    private final IdempotencyService service;

    public PaymentController(IdempotencyService service) {
        this.service = service;
    }

    @PostMapping("/process-payment")
    public ResponseEntity<PaymentResponse> process(
            @RequestHeader("Idempotency-Key") @NotBlank String key,
            @Valid @RequestBody PaymentRequest body) {
        IdempotencyResult result = service.handle(key, body);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(result.entry().statusCode());
        if (result.cacheHit()) {
            // Per User Story 2: signal that this is a replayed response.
            builder.header("X-Cache-Hit", "true");
        }
        return builder.body(result.entry().body());
    }
}
