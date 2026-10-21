package com.petlog.events.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateWalkRequest(

    @NotNull
    OffsetDateTime startTime,

    @NotNull
    OffsetDateTime endTime,

    Long participantId,

    String comment
) {
}
