package com.petlog.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateNoteRequest(

    @NotNull
    OffsetDateTime occurredAt,

    @NotBlank
    @Size(max = 150)
    String title,

    String description

) {
}
