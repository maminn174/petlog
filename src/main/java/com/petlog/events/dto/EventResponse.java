package com.petlog.events.dto;

import com.petlog.events.Event;
import com.petlog.events.EventType;

import java.time.OffsetDateTime;
import java.util.Map;

public record EventResponse(
    Long id,
    Long petId,
    Long createdBy,
    EventType type,
    OffsetDateTime occurredAt,
    Map<String, Object> data,
    OffsetDateTime createdAt
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
            event.getId(),
            event.getPet().getId(),
            event.getCreatedBy().getId(),
            event.getType(),
            event.getOccurredAt(),
            event.getData(),
            event.getCreatedAt()
        );
    }
}
