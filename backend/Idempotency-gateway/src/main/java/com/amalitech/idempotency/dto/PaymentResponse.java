package com.amalitech.idempotency.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentResponse(
        @Schema(example = "Charged 100 USD")
        String message
) {
}
