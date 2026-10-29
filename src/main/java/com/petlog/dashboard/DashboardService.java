package com.petlog.dashboard;

import com.petlog.dashboard.dto.DashboardResponse;
import com.petlog.events.Event;
import com.petlog.events.EventRepository;
import com.petlog.events.EventService;
import com.petlog.events.EventType;
import com.petlog.events.dto.EventResponse;
import com.petlog.pet.PetService;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final PetService petService;
    private final EventService eventService;
    private final Clock clock;

    public DashboardService(EventRepository eventRepository,
                            UserService userService,
                            PetService petService,
                            EventService eventService,
                            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.petService = petService;
        this.eventService = eventService;
        this.clock = clock;
    }

    private BigDecimal extractWeight(Object weight) {
        if (weight == null) {
            return null;
        }

        if (weight instanceof Number number) {
            return new BigDecimal(number.toString());
        }

        throw new IllegalArgumentException(
            "Weight must be a number"
        );
    }

    private BigDecimal getLastWeight(Long petId) {
        return eventRepository
            .findFirstByDeletedAtIsNullAndPet_IdAndTypeOrderByOccurredAtDesc(
                petId,
                EventType.WEIGHT
            )
            .map(event -> extractWeight(event.getData().get("weightKg")))
            .orElse(null);
    }

    private OffsetDateTime getLastFeedingAt(Long petId) {
        return eventRepository
            .findFirstByDeletedAtIsNullAndPet_IdAndTypeOrderByOccurredAtDesc(
                petId,
                EventType.FEEDING
            )
            .map(Event::getOccurredAt)
            .orElse(null);
    }

    private long getTodayWalkDurationMinutes(Long petId) {
        LocalDate today = LocalDate.now(clock);

        ZoneId zone = clock.getZone();

        OffsetDateTime startOfDay = today.atStartOfDay(zone).toOffsetDateTime();

        OffsetDateTime endOfDay = today.plusDays(1)
            .atStartOfDay(zone)
            .toOffsetDateTime();

        List<Event> events = eventRepository.findFinishedWalksForPeriod(petId, startOfDay, endOfDay);

        long totalMinutes = 0;

        for (Event event : events) {
            OffsetDateTime startTime = OffsetDateTime.parse(
                (String) event.getData().get("startTime")
            );

            OffsetDateTime endTime = OffsetDateTime.parse(
                (String) event.getData().get("endTime")
            );

            long minutes = Duration.between(startTime, endTime).toMinutes();

            totalMinutes += minutes;
        }

        return totalMinutes;
    }

    public DashboardResponse getDashboard(Long petId, String email) {

        User user = userService.getByEmail(email);
        petService.getByIdForUser(petId, user);

        BigDecimal lastWeight = getLastWeight(petId);
        OffsetDateTime lastFeedingAt = getLastFeedingAt(petId);
        long todayWalkDurationMinutes = getTodayWalkDurationMinutes(petId);

        EventResponse activeWalk = eventService
            .getActiveWalk(petId, email)
            .map(EventResponse::from)
            .orElse(null);

        return new DashboardResponse(
            todayWalkDurationMinutes,
            lastFeedingAt,
            lastWeight,
            activeWalk
        );
    }
}
