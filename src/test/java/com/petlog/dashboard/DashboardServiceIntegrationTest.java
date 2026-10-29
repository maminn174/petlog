package com.petlog.dashboard;

import com.petlog.dashboard.dto.DashboardResponse;
import com.petlog.events.Event;
import com.petlog.events.EventRepository;
import com.petlog.events.EventType;
import com.petlog.pet.Pet;
import com.petlog.pet.PetRepository;
import com.petlog.petmember.PetMember;
import com.petlog.petmember.PetMemberRepository;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import com.petlog.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DashboardServiceIntegrationTest.FixedClockConfig.class)
@Testcontainers
@Transactional
class DashboardServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private PetMemberRepository petMemberRepository;
    @Autowired
    private EntityManager entityManager;

    private User owner;
    private Pet pet;
    @Autowired
    private Clock clock;
    @Autowired
    private MockMvc mockMvc;
    private OffsetDateTime startOfDay;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("Owner", "dashboard-owner@example.com", "hash"));
        pet = petRepository.save(new Pet("Buddy", "DOG"));
        petMemberRepository.save(new PetMember(owner, pet, Role.OWNER));
        startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toOffsetDateTime();
    }

    @Test
    void shouldIgnoreDeletedEvents() {
        weight(startOfDay.plusHours(8), "12.5");
        OffsetDateTime feedingAt = startOfDay.plusHours(9);
        feeding(feedingAt);
        walk(startOfDay.plusHours(10), startOfDay.plusHours(10).plusMinutes(20));
        softDelete(weight(startOfDay.plusHours(12), "99"));
        softDelete(feeding(startOfDay.plusHours(13)));
        softDelete(walk(startOfDay.plusHours(14), startOfDay.plusHours(16)));
        softDelete(walk(startOfDay.plusHours(17), null));

        DashboardResponse result = dashboard();

        assertEquals(0, new BigDecimal("12.5").compareTo(result.lastWeight()));
        assertEquals(feedingAt.toInstant(), result.lastFeedingAt().toInstant());
        assertEquals(20, result.todayWalkDurationMinutes());
        assertNull(result.activeWalk());
    }

    @Test
    void shouldIgnoreActiveWalkInTotalDuration() {
        walk(startOfDay.plusHours(8), startOfDay.plusHours(8).plusMinutes(30));
        Event active = walk(startOfDay.plusHours(10), null);

        DashboardResponse result = dashboard();

        assertEquals(30, result.todayWalkDurationMinutes());
        assertNotNull(result.activeWalk());
        assertEquals(active.getId(), result.activeWalk().id());
    }

    @Test
    void shouldIgnoreWalksFromOtherDays() {
        walk(startOfDay.minusDays(1).plusHours(8), startOfDay.minusDays(1).plusHours(9));
        // The lower boundary is inclusive, the upper boundary is exclusive.
        walk(startOfDay, startOfDay.plusMinutes(15));
        walk(startOfDay.plusDays(1), startOfDay.plusDays(1).plusHours(2));

        assertEquals(15, dashboard().todayWalkDurationMinutes());
    }

    @Test
    void shouldCountWalkCrossingMidnightByStartDate() {
        walk(startOfDay.minusMinutes(20), startOfDay.plusMinutes(10));
        walk(startOfDay.plusHours(23).plusMinutes(40), startOfDay.plusDays(1).plusMinutes(20));

        assertEquals(40, dashboard().todayWalkDurationMinutes());
    }

    @Test
    void shouldReturnLatestEventByOccurredAtNotCreatedAt() {
        Event latestWeight = weight(startOfDay.plusHours(12), "12.5");
        Event latestFeeding = feeding(startOfDay.plusHours(13));
        Event backdatedWeight = weight(startOfDay.plusHours(8), "10");
        Event backdatedFeeding = feeding(startOfDay.plusHours(9));
        // Explicit timestamps avoid relying on database clock precision or insert timing.
        setCreatedAt(latestWeight, startOfDay.minusDays(2));
        setCreatedAt(latestFeeding, startOfDay.minusDays(2));
        setCreatedAt(backdatedWeight, startOfDay.minusDays(1));
        setCreatedAt(backdatedFeeding, startOfDay.minusDays(1));
        entityManager.clear();

        DashboardResponse result = dashboard();

        assertEquals(0, new BigDecimal("12.5").compareTo(result.lastWeight()));
        assertEquals(latestFeeding.getOccurredAt().toInstant(), result.lastFeedingAt().toInstant());
    }

    @Test
    void shouldAllowOwnerToViewDashboard() {
        assertCanViewDashboard(owner);
    }

    @Test
    void shouldAllowMemberToViewDashboard() {
        assertCanViewDashboard(createMember(Role.MEMBER));
    }

    @Test
    void shouldAllowViewerToViewDashboard() {
        assertCanViewDashboard(createMember(Role.VIEWER));
    }

    @Test
    void shouldReturnDashboardThroughHttp() throws Exception {
        weight(startOfDay.plusHours(8), "12.5");
        OffsetDateTime feedingAt = startOfDay.plusHours(9);
        feeding(feedingAt);
        walk(startOfDay.plusHours(10), startOfDay.plusHours(10).plusMinutes(35));
        Event active = walk(startOfDay.plusHours(12), null);
        entityManager.flush();

        mockMvc.perform(get("/api/pets/{petId}/dashboard", pet.getId())
                .with(user(owner.getEmail())))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.todayWalkDurationMinutes").value(35))
            .andExpect(jsonPath("$.lastWeight").value(12.5))
            .andExpect(jsonPath("$.lastFeedingAt").value(feedingAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
            .andExpect(jsonPath("$.activeWalk.id").value(active.getId()))
            .andExpect(jsonPath("$.activeWalk.petId").value(pet.getId()))
            .andExpect(jsonPath("$.activeWalk.createdBy").value(owner.getId()))
            .andExpect(jsonPath("$.activeWalk.type").value("WALK"))
            .andExpect(jsonPath("$.activeWalk.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.activeWalk.data.startTime").value(active.getData().get("startTime")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            // UTC date differs from Moscow date, exercising the clock's calendar zone.
            return Clock.fixed(Instant.parse("2026-10-28T22:30:00Z"), ZoneId.of("Europe/Moscow"));
        }
    }

    private DashboardResponse dashboard() {
        return dashboardFor(owner);
    }

    private DashboardResponse dashboardFor(User user) {
        entityManager.flush();
        entityManager.clear();
        return dashboardService.getDashboard(pet.getId(), user.getEmail());
    }

    private Event weight(OffsetDateTime occurredAt, String kg) {
        return saveEvent(EventType.WEIGHT, occurredAt, Map.of("weightKg", new BigDecimal(kg)));
    }

    private Event feeding(OffsetDateTime occurredAt) {
        return saveEvent(EventType.FEEDING, occurredAt, Map.of());
    }

    private Event walk(OffsetDateTime start, OffsetDateTime end) {
        Map<String, Object> data = new HashMap<>();
        data.put("startTime", start.toString());
        data.put("endTime", end == null ? null : end.toString());
        data.put("status", end == null ? "ACTIVE" : "FINISHED");
        data.put("participantId", owner.getId());
        return saveEvent(EventType.WALK, start, data);
    }

    private Event saveEvent(EventType type, OffsetDateTime occurredAt, Map<String, Object> data) {
        return eventRepository.saveAndFlush(new Event(owner, pet, type, occurredAt, data));
    }

    private void softDelete(Event event) {
        event.setDeletedAt(startOfDay.plusDays(2));
        eventRepository.saveAndFlush(event);
    }

    private void setCreatedAt(Event event, OffsetDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE events SET created_at = :createdAt WHERE id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", event.getId())
            .executeUpdate();
    }

    private User createMember(Role role) {
        User user = userRepository.save(new User(role.name(), role.name() + "@example.com", "hash"));
        petMemberRepository.save(new PetMember(user, pet, role));
        return user;
    }

    private void assertCanViewDashboard(User user) {
        weight(startOfDay.plusHours(8), "12.5");
        Event active = walk(startOfDay.plusHours(10), null);

        DashboardResponse result = assertDoesNotThrow(() -> dashboardFor(user));

        assertEquals(0, new BigDecimal("12.5").compareTo(result.lastWeight()));
        assertNotNull(result.activeWalk());
        assertEquals(active.getId(), result.activeWalk().id());
    }

}
