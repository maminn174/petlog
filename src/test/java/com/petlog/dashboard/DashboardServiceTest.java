package com.petlog.dashboard;

import com.petlog.dashboard.dto.DashboardResponse;
import com.petlog.events.Event;
import com.petlog.events.EventRepository;
import com.petlog.events.EventService;
import com.petlog.events.EventType;
import com.petlog.events.dto.EventResponse;
import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long PET_ID = 1L;
    private static final String EMAIL = "owner@example.com";
    private static final Clock FIXED_CLOCK =
        Clock.fixed(Instant.parse("2026-10-28T22:30:00Z"), ZoneId.of("Europe/Moscow"));
    private static final OffsetDateTime START_OF_DAY = OffsetDateTime.parse("2026-10-29T00:00:00+03:00");

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserService userService;
    @Mock
    private PetService petService;
    @Mock
    private EventService eventService;
    @Mock
    private Clock clock;
    @InjectMocks
    private DashboardService dashboardService;

    private final User user = new User("Owner", EMAIL, "hash");
    private final Pet pet = new Pet("Buddy", "DOG");

    @BeforeEach
    void setUp() {
        when(userService.getByEmail(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldReturnEmptyDashboardWhenNoEventsExist() {
        configureClock();

        DashboardResponse result = dashboard();

        assertEquals(0, result.todayWalkDurationMinutes());
        assertNull(result.lastWeight());
        assertNull(result.lastFeedingAt());
        assertNull(result.activeWalk());
    }

    @Test
    void shouldReturnLatestWeight() {
        configureClock();
        Event weight = new Event(user, pet, EventType.WEIGHT, START_OF_DAY, Map.of("weightKg", 12.75));
        when(eventRepository.findFirstByDeletedAtIsNullAndPet_IdAndTypeOrderByOccurredAtDesc(
            PET_ID, EventType.WEIGHT)).thenReturn(Optional.of(weight));

        assertEquals(new BigDecimal("12.75"), dashboard().lastWeight());
    }

    @Test
    void shouldReturnLatestFeeding() {
        configureClock();
        when(eventRepository.findFirstByDeletedAtIsNullAndPet_IdAndTypeOrderByOccurredAtDesc(
            PET_ID, EventType.WEIGHT)).thenReturn(Optional.empty());
        OffsetDateTime feedingAt = START_OF_DAY.plusHours(9);
        Event feeding = new Event(user, pet, EventType.FEEDING, feedingAt, Map.of());
        when(eventRepository.findFirstByDeletedAtIsNullAndPet_IdAndTypeOrderByOccurredAtDesc(
            PET_ID, EventType.FEEDING)).thenReturn(Optional.of(feeding));

        assertEquals(feedingAt, dashboard().lastFeedingAt());
    }

    @Test
    void shouldCalculateTotalWalkDuration() {
        configureClock();
        when(eventRepository.findFinishedWalksForPeriod(PET_ID, START_OF_DAY, START_OF_DAY.plusDays(1)))
            .thenReturn(List.of(walk(8, 25), walk(12, 40), walk(18, 15)));

        assertEquals(80, dashboard().todayWalkDurationMinutes());
        verify(eventRepository).findFinishedWalksForPeriod(PET_ID, START_OF_DAY, START_OF_DAY.plusDays(1));
    }

    @Test
    void shouldReturnZeroWhenNoWalksToday() {
        configureClock();
        when(eventRepository.findFinishedWalksForPeriod(PET_ID, START_OF_DAY, START_OF_DAY.plusDays(1)))
            .thenReturn(List.of());

        assertEquals(0, dashboard().todayWalkDurationMinutes());
    }

    @Test
    void shouldReturnActiveWalk() {
        configureClock();
        Event active = mock(Event.class);
        User creator = mock(User.class);
        Pet activePet = mock(Pet.class);
        OffsetDateTime startedAt = START_OF_DAY.plusHours(10);
        OffsetDateTime createdAt = startedAt.plusSeconds(5);
        Map<String, Object> data = Map.of("status", "ACTIVE", "startTime", startedAt.toString());
        when(active.getId()).thenReturn(42L);
        when(active.getPet()).thenReturn(activePet);
        when(activePet.getId()).thenReturn(PET_ID);
        when(active.getCreatedBy()).thenReturn(creator);
        when(creator.getId()).thenReturn(7L);
        when(active.getType()).thenReturn(EventType.WALK);
        when(active.getOccurredAt()).thenReturn(startedAt);
        when(active.getCreatedAt()).thenReturn(createdAt);
        when(active.getData()).thenReturn(data);
        when(eventService.getActiveWalk(PET_ID, EMAIL)).thenReturn(Optional.of(active));

        assertEquals(new EventResponse(42L, PET_ID, 7L, EventType.WALK, startedAt, data, createdAt),
            dashboard().activeWalk());
    }

    @Test
    void shouldReturnNullWhenNoActiveWalk() {
        configureClock();
        when(eventService.getActiveWalk(PET_ID, EMAIL)).thenReturn(Optional.empty());

        assertNull(dashboard().activeWalk());
    }

    @Test
    void shouldDenyAccessToUnauthorizedUser() {
        ResponseStatusException denied = new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        when(petService.getByIdForUser(PET_ID, user)).thenThrow(denied);

        assertSame(denied, assertThrows(ResponseStatusException.class, this::dashboard));
        verifyNoInteractions(eventRepository, eventService, clock);
    }

    private void configureClock() {
        when(clock.instant()).thenReturn(FIXED_CLOCK.instant());
        when(clock.getZone()).thenReturn(FIXED_CLOCK.getZone());
    }

    private DashboardResponse dashboard() {
        return dashboardService.getDashboard(PET_ID, EMAIL);
    }

    private Event walk(int startHour, int minutes) {
        OffsetDateTime start = START_OF_DAY.plusHours(startHour);
        return new Event(user, pet, EventType.WALK, start, Map.of(
            "startTime", start.toString(),
            "endTime", start.plusMinutes(minutes).toString(),
            "status", "FINISHED"
        ));
    }
}
