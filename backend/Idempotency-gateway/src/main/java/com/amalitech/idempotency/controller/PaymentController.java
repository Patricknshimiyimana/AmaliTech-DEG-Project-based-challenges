package com.amalitech.idempotency.controller;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.service.IdempotencyService;
import com.amalitech.idempotency.store.CachedResponse;
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
        CachedResponse result = service.handle(key, body);
        return ResponseEntity.status(result.statusCode()).body(result.body());
    }
}
