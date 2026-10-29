package com.petlog.dashboard.dto;

import com.petlog.events.dto.EventResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardResponse(
    long todayWalkDurationMinutes,
    OffsetDateTime lastFeedingAt,
    BigDecimal lastWeight,
    EventResponse activeWalk
) {
}
