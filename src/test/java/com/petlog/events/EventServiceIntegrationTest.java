package com.petlog.events;

import com.petlog.pet.Pet;
import com.petlog.common.exception.GlobalExceptionHandler;
import com.petlog.events.dto.StartWalkRequest;
import com.petlog.pet.PetRepository;
import com.petlog.petmember.PetMember;
import com.petlog.petmember.PetMemberRepository;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import com.petlog.user.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
class EventServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetMemberRepository petMemberRepository;

    @MockitoSpyBean
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldRejectSecondActiveWalkForSamePet() {
        User user = createUser();
        Pet pet = createPetFor(user, "Busya");
        Event first = eventRepository.saveAndFlush(activeWalk(user, pet));

        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> eventRepository.saveAndFlush(activeWalk(user, pet))
        );

        Throwable cause = exception.getCause();
        while (cause != null && !(cause instanceof ConstraintViolationException)) {
            cause = cause.getCause();
        }
        ConstraintViolationException violation = assertInstanceOf(ConstraintViolationException.class, cause);
        assertEquals("uq_events_active_walk_per_pet", violation.getConstraintName());
        assertEquals("23505", violation.getSQLState());
        assertEquals(1, eventRepository.count());
        assertEquals(first.getId(), eventRepository.findActiveWalkByPetId(pet.getId()).orElseThrow().getId());
    }

    @Test
    void shouldAllowActiveWalksForDifferentPets() {
        User user = createUser();
        Pet firstPet = createPetFor(user, "Busya");
        Pet secondPet = createPetFor(user, "Rex");

        Event first = eventService.startWalk(firstPet.getId(), user.getEmail(), new StartWalkRequest(null));
        Event second = eventService.startWalk(secondPet.getId(), user.getEmail(), new StartWalkRequest(null));

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, eventRepository.count());
        assertEquals(first.getId(), eventRepository.findActiveWalkByPetId(firstPet.getId()).orElseThrow().getId());
        assertEquals(second.getId(), eventRepository.findActiveWalkByPetId(secondPet.getId()).orElseThrow().getId());
    }

    @Test
    void shouldAllowNewWalkAfterFinishingPrevious() {
        User user = createUser();
        Pet pet = createPetFor(user, "Busya");
        Event first = eventService.startWalk(pet.getId(), user.getEmail(), new StartWalkRequest(null));

        eventService.finishWalk(pet.getId(), first.getId(), user.getEmail());
        Event second = eventService.startWalk(pet.getId(), user.getEmail(), new StartWalkRequest(null));

        Event finished = eventRepository.findById(first.getId()).orElseThrow();
        assertEquals("FINISHED", finished.getData().get("status"));
        assertNotNull(finished.getData().get("endTime"));
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, eventRepository.count());
        assertEquals(second.getId(), eventRepository.findActiveWalkByPetId(pet.getId()).orElseThrow().getId());
    }

    @Test
    void shouldAllowNewWalkAfterSoftDelete() {
        User user = createUser();
        Pet pet = createPetFor(user, "Busya");
        Event first = eventService.startWalk(pet.getId(), user.getEmail(), new StartWalkRequest(null));

        eventService.delete(pet.getId(), first.getId(), user.getEmail());
        Event second = eventService.startWalk(pet.getId(), user.getEmail(), new StartWalkRequest(null));

        Event deleted = eventRepository.findById(first.getId()).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
        assertEquals("ACTIVE", deleted.getData().get("status"));
        assertTrue(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(first.getId(), pet.getId()).isEmpty());
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, eventRepository.count());
        assertEquals(second.getId(), eventRepository.findActiveWalkByPetId(pet.getId()).orElseThrow().getId());
    }

    @ParameterizedTest(name = "databaseConstraintConflict={0}")
    @ValueSource(booleans = {false, true})
    void shouldReturnConflictWhenStartingSecondWalk(boolean databaseConstraintConflict) throws Exception {
        User user = createUser();
        Pet pet = createPetFor(user, "Busya");
        String url = "/api/pets/" + pet.getId() + "/events/walk/start";
        var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);

        mockMvc.perform(post(url).principal(authentication)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        if (databaseConstraintConflict) {
            // Simulate a stale pre-check during concurrent starts; the insert still reaches PostgreSQL.
            doReturn(Optional.empty()).when(eventRepository).findActiveWalkByPetId(pet.getId());
        }

        mockMvc.perform(post(url).principal(authentication)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(
                databaseConstraintConflict ? "Pet already has active walk"
                    : "Pet with id " + pet.getId() + " already has active walk"))
            .andExpect(result -> {
                if (databaseConstraintConflict) {
                    assertInstanceOf(DataIntegrityViolationException.class, result.getResolvedException());
                }
            });

        assertEquals(1, eventRepository.count());
    }

    private User createUser() {
        return userRepository.save(new User("Test User", "active-walk@example.com", "test-password-hash"));
    }

    private Pet createPetFor(User user, String name) {
        Pet pet = petRepository.save(new Pet(name, "DOG"));
        petMemberRepository.save(new PetMember(user, pet, Role.OWNER));
        return pet;
    }

    private Event activeWalk(User user, Pet pet) {
        OffsetDateTime startTime = OffsetDateTime.now().minusMinutes(30).truncatedTo(ChronoUnit.MICROS);
        Map<String, Object> data = new HashMap<>();
        data.put("startTime", startTime.toString());
        data.put("endTime", null);
        data.put("status", "ACTIVE");
        data.put("participantId", user.getId());
        return new Event(user, pet, EventType.WALK, startTime, data);
    }

    @AfterEach
    void cleanUp() {
        eventRepository.deleteAllInBatch();
        petMemberRepository.deleteAllInBatch();
        petRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldPersistFinishedWalkToDatabase() {

        // 1. Создаём пользователя
        User user = userRepository.save(
            new User(
                "Test User",
                "walk-integration@example.com",
                "test-password-hash"
            )
        );

        // 2. Создаём питомца
        Pet pet = petRepository.save(
            new Pet("Busya", "DOG")
        );

        // 3. Назначаем пользователю роль OWNER
        petMemberRepository.save(
            new PetMember(user, pet, Role.OWNER)
        );

        // 4. Создаём активную прогулку
        OffsetDateTime startTime = OffsetDateTime.now()
            .minusMinutes(30)
            .truncatedTo(ChronoUnit.MICROS);

        Map<String, Object> data = new HashMap<>();
        data.put("startTime", startTime.toString());
        data.put("endTime", null);
        data.put("status", "ACTIVE");
        data.put("participantId", user.getId());

        Event walk = eventRepository.save(
            new Event(
                user,
                pet,
                EventType.WALK,
                startTime,
                data
            )
        );

        Long walkId = walk.getId();

        // 5. Вызываем реальный EventService
        eventService.finishWalk(
            pet.getId(),
            walkId,
            user.getEmail()
        );

        // 6. Повторно читаем событие из PostgreSQL
        Event savedWalk = eventRepository.findById(walkId)
            .orElseThrow();

        // 7. Проверяем сохранённое состояние
        assertEquals(EventType.WALK, savedWalk.getType());

        assertEquals(
            "FINISHED",
            savedWalk.getData().get("status")
        );

        assertNotNull(savedWalk.getData().get("endTime"));

        OffsetDateTime endTime = OffsetDateTime.parse(
            (String) savedWalk.getData().get("endTime")
        );

        assertTrue(endTime.isAfter(startTime));

        assertEquals(
            startTime.toInstant(),
            savedWalk.getOccurredAt().toInstant()
        );

        assertEquals(
            user.getId(),
            ((Number) savedWalk.getData()
                .get("participantId")).longValue()
        );
    }
}
