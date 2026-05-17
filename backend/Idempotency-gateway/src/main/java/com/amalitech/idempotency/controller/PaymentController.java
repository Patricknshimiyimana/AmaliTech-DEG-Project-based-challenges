package com.amalitech.idempotency.controller;

import com.amalitech.idempotency.dto.PaymentRequest;
import com.amalitech.idempotency.dto.PaymentResponse;
import com.amalitech.idempotency.exception.ApiError;
import com.amalitech.idempotency.service.IdempotencyResult;
import com.amalitech.idempotency.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Process a payment with idempotency guarantees")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Processed (or replayed; see X-Cache-Hit header)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed: missing/blank Idempotency-Key or invalid body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency-Key reused with a different request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    public ResponseEntity<PaymentResponse> process(
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Parameter(
                    description = "Client-generated unique value (e.g. UUID v4) for this payment attempt. "
                            + "Reuse the same value on retries of the same intent; generate a new one for a new payment.",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            String key,
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
