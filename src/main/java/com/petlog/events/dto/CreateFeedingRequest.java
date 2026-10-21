package com.petlog.events.dto;

import com.petlog.events.Appetite;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record CreateFeedingRequest(

    @Positive
    Integer amountGrams,

    Appetite appetite,

    String comment,

    @NotNull
    OffsetDateTime occurredAt
) {
}
