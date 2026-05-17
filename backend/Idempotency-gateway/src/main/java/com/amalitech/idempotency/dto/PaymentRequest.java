package com.amalitech.idempotency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull @Positive
        @Schema(example = "100")
        BigDecimal amount,

        @NotBlank @Size(min = 3, max = 3)
        @Schema(example = "USD")
        String currency
) {
}
