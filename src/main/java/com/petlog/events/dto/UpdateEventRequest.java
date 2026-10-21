package com.petlog.events.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record UpdateEventRequest(

    OffsetDateTime occurredAt,

    Map<String, Object> data
) {
}
