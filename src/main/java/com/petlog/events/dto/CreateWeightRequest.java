package com.petlog.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateWeightRequest(

    @NotNull
    OffsetDateTime occurredAt,

    @NotNull
    @Positive
    BigDecimal weightKg,

    String comment
) {
}
