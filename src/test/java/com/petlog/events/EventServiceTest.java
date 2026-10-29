package com.petlog.events;

import com.petlog.events.dto.*;
import com.petlog.events.exception.ActiveWalkAlreadyExistsException;
import com.petlog.events.exception.EventNotFoundException;
import com.petlog.events.exception.EventValidationException;
import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.petmember.PetMemberService;
import com.petlog.petmember.Role;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PetService petService;

    @Mock
    private UserService userService;

    @Mock
    private PetMemberService petMemberService;

    @Mock
    private EventValidator eventValidator;

    @InjectMocks
    private EventService eventService;

    private static final String EMAIL = "test@example.com";
    private static final Long PET_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final OffsetDateTime OCCURRED_AT =
        OffsetDateTime.parse("2026-10-22T10:00:00+03:00");

    @Mock
    private User user;

    @Mock
    private Pet pet;

    private void allowCreation() {
        when(userService.getByEmail(EMAIL)).thenReturn(user);
        when(petService.getByIdForUser(PET_ID, user)).thenReturn(pet);
    }

    private void configureEventAccess(boolean hasAccess) {
        when(userService.getByEmail(EMAIL)).thenReturn(user);
        when(petService.getById(PET_ID)).thenReturn(pet);
        when(petMemberService.hasAccess(user, pet)).thenReturn(hasAccess);
    }

    private Event feedingEvent() {
        return new Event(user, pet, EventType.FEEDING, OCCURRED_AT,
            new HashMap<>(Map.of("amountGrams", 80, "comment", "Morning feeding")));
    }

    private Event activeWalk() {
        OffsetDateTime startTime = OffsetDateTime.now().minusHours(1);
        return new Event(user, pet, EventType.WALK, startTime,
            new HashMap<>(Map.of("startTime", startTime.toString(), "status", "ACTIVE")));
    }

    private void configureRole(Role role) {
        when(petMemberService.hasRole(user, pet, Role.OWNER)).thenReturn(role == Role.OWNER);
        when(petMemberService.hasRole(user, pet, Role.MEMBER)).thenReturn(role == Role.MEMBER);
    }

    @Nested
    class WalkLifecycle {

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {2L})
        void shouldStartWalkSuccessfully(Long participantId) {
            allowCreation();
            Long authorId = 3L;
            Long expectedParticipantId = participantId != null ? participantId : authorId;
            if (participantId == null) {
                when(user.getId()).thenReturn(authorId);
            }
            User participant = mock(User.class);
            when(userService.getById(expectedParticipantId)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(true);
            when(eventRepository.findActiveWalkByPetId(PET_ID)).thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            OffsetDateTime beforeStart = OffsetDateTime.now();

            Event result = eventService.startWalk(PET_ID, EMAIL, new StartWalkRequest(participantId));

            OffsetDateTime afterStart = OffsetDateTime.now();
            OffsetDateTime startTime = OffsetDateTime.parse((String) result.getData().get("startTime"));
            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.WALK, result.getType());
            assertEquals("ACTIVE", result.getData().get("status"));
            assertEquals(expectedParticipantId, result.getData().get("participantId"));
            assertEquals(startTime, result.getOccurredAt());
            assertFalse(startTime.isBefore(beforeStart));
            assertFalse(startTime.isAfter(afterStart));
            assertNull(result.getData().get("endTime"));
            verify(petMemberService).requireMemberOrOwner(user, pet);
            verify(userService).getById(expectedParticipantId);
            verify(petMemberService).hasAccess(participant, pet);
            verify(eventRepository).save(result);
        }

        @Test
        void shouldRejectStartWalkWhenAlreadyActive() {
            allowCreation();
            when(eventRepository.findActiveWalkByPetId(PET_ID)).thenReturn(Optional.of(activeWalk()));

            assertThrows(ActiveWalkAlreadyExistsException.class,
                () -> eventService.startWalk(PET_ID, EMAIL, new StartWalkRequest(2L)));

            verify(eventRepository, never()).save(any(Event.class));
            verify(userService, never()).getById(any());
        }

        @Test
        void shouldRejectStartWalkWithInvalidParticipant() {
            allowCreation();
            User participant = mock(User.class);
            when(eventRepository.findActiveWalkByPetId(PET_ID)).thenReturn(Optional.empty());
            when(userService.getById(2L)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(false);

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.startWalk(PET_ID, EMAIL, new StartWalkRequest(2L)));

            assertEquals("Participant is not a member of this pet", exception.getMessage());
            verify(petMemberService).hasAccess(participant, pet);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectStartWalkWhenViewer() {
            allowCreation();
            configureRole(Role.VIEWER);
            doCallRealMethod().when(petMemberService).requireMemberOrOwner(user, pet);

            assertThrows(AccessDeniedException.class,
                () -> eventService.startWalk(PET_ID, EMAIL, new StartWalkRequest(null)));

            verify(petMemberService).requireMemberOrOwner(user, pet);
            verifyNoInteractions(eventRepository);
        }

        @Test
        void shouldFinishWalkSuccessfully() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            String startTime = (String) event.getData().get("startTime");
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            OffsetDateTime beforeFinish = OffsetDateTime.now();

            EventResponse response = eventService.finishWalk(PET_ID, EVENT_ID, EMAIL);

            OffsetDateTime afterFinish = OffsetDateTime.now();
            assertEquals("FINISHED", event.getData().get("status"));
            assertEquals(event.getData(), response.data());
            assertEquals(startTime, event.getData().get("startTime"));
            OffsetDateTime endTime = OffsetDateTime.parse((String) response.data().get("endTime"));
            assertTrue(endTime.isAfter(OffsetDateTime.parse(startTime)));
            assertFalse(endTime.isBefore(beforeFinish));
            assertFalse(endTime.isAfter(afterFinish));
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectFinishWalkWhenNotActive() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            event.getData().put("status", "FINISHED");
            event.getData().put("endTime", event.getOccurredAt().plusMinutes(30).toString());
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.finishWalk(PET_ID, EVENT_ID, EMAIL));

            assertEquals("Walk is not active", exception.getMessage());
            assertEquals(originalData, event.getData());
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectFinishWalkForNonWalkEvent() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.finishWalk(PET_ID, EVENT_ID, EMAIL));

            assertEquals("Event is not a walk", exception.getMessage());
            assertEquals(originalData, event.getData());
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectFinishWalkWhenAccessDenied() {
            configureEventAccess(false);

            assertThrows(AccessDeniedException.class,
                () -> eventService.finishWalk(PET_ID, EVENT_ID, EMAIL));

            verifyNoInteractions(eventRepository);
        }

        @Test
        void shouldGetActiveWalkSuccessfully() {
            allowCreation();
            Event event = activeWalk();
            when(eventRepository.findActiveWalkByPetId(PET_ID)).thenReturn(Optional.of(event));

            Optional<Event> result = eventService.getActiveWalk(PET_ID, EMAIL);

            assertEquals(Optional.of(event), result);
            verify(petService).getByIdForUser(PET_ID, user);
            verify(eventRepository).findActiveWalkByPetId(PET_ID);
        }

        @Test
        void shouldReturnEmptyWhenNoActiveWalk() {
            allowCreation();
            when(eventRepository.findActiveWalkByPetId(PET_ID)).thenReturn(Optional.empty());

            assertEquals(Optional.empty(), eventService.getActiveWalk(PET_ID, EMAIL));

            verify(petService).getByIdForUser(PET_ID, user);
            verify(eventRepository).findActiveWalkByPetId(PET_ID);
        }
    }

    private void allowEventModification() {
        configureRole(Role.OWNER);
        when(user.getId()).thenReturn(1L);
    }

    @Nested
    @DisplayName("Создание событий")
    class Creation {

        @Test
        void shouldNotCreateFeedingWhenViewer() {
            allowCreation();
            configureRole(Role.VIEWER);
            doCallRealMethod().when(petMemberService).requireMemberOrOwner(user, pet);
            CreateFeedingRequest request = new CreateFeedingRequest(80, Appetite.GOOD, "Morning feeding", OCCURRED_AT);

            assertThrows(AccessDeniedException.class,
                () -> eventService.createFeeding(PET_ID, EMAIL, request));

            verify(petMemberService).requireMemberOrOwner(user, pet);
            verifyNoInteractions(eventRepository, eventValidator);
        }

        @Test
        void shouldCreateFeedingWhenMember() {
            allowCreation();
            configureRole(Role.MEMBER);
            doCallRealMethod().when(petMemberService).requireMemberOrOwner(user, pet);
            CreateFeedingRequest request = new CreateFeedingRequest(80, Appetite.GOOD, "Morning feeding", OCCURRED_AT);
            Map<String, Object> expectedData = Map.of(
                "amountGrams", 80, "appetite", "GOOD", "comment", "Morning feeding"
            );
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createFeeding(PET_ID, EMAIL, request);

            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.FEEDING, result.getType());
            assertEquals(OCCURRED_AT, result.getOccurredAt());
            assertEquals(expectedData, result.getData());
            verify(petMemberService).requireMemberOrOwner(user, pet);
            verify(eventValidator).validateFeeding(expectedData);
            verify(eventRepository).save(result);
        }

        @Test
        void shouldCreateFeedingWhenOwner() {
            allowCreation();
            when(petMemberService.hasRole(user, pet, Role.OWNER)).thenReturn(true);
            doCallRealMethod().when(petMemberService).requireMemberOrOwner(user, pet);
            CreateFeedingRequest request = new CreateFeedingRequest(80, Appetite.GOOD, "Morning feeding", OCCURRED_AT);
            Map<String, Object> expectedData = Map.of(
                "amountGrams", 80, "appetite", "GOOD", "comment", "Morning feeding"
            );
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createFeeding(PET_ID, EMAIL, request);

            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.FEEDING, result.getType());
            assertEquals(OCCURRED_AT, result.getOccurredAt());
            assertEquals(expectedData, result.getData());
            verify(petMemberService).requireMemberOrOwner(user, pet);
            verify(eventValidator).validateFeeding(expectedData);
            verify(eventRepository).save(result);
        }

        @Test
        void shouldCreateFeedingSuccessfully() {
            allowCreation();
            CreateFeedingRequest request = new CreateFeedingRequest(80, Appetite.GOOD, "Morning feeding", OCCURRED_AT);
            Map<String, Object> expectedData = Map.of(
                "amountGrams", 80, "appetite", "GOOD", "comment", "Morning feeding"
            );
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createFeeding(PET_ID, EMAIL, request);

            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.FEEDING, result.getType());
            assertEquals(OCCURRED_AT, result.getOccurredAt());
            assertEquals(expectedData, result.getData());
            verify(eventValidator).validateFeeding(expectedData);
            verify(eventRepository).save(result);
        }

        @Test
        void shouldNotCreateFeedingWhenAccessDenied() {
            when(userService.getByEmail(EMAIL)).thenReturn(user);
            when(petService.getByIdForUser(PET_ID, user))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
            CreateFeedingRequest request = new CreateFeedingRequest(80, null, "Morning feeding", OCCURRED_AT);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> eventService.createFeeding(PET_ID, EMAIL, request));

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
            verifyNoInteractions(eventRepository, eventValidator);
        }

        @Test
        void shouldNotCreateFeedingWithInvalidAmount() {
            allowCreation();
            CreateFeedingRequest request = new CreateFeedingRequest(-50, null, "Morning feeding", OCCURRED_AT);
            Map<String, Object> invalidData = Map.of("amountGrams", -50, "comment", "Morning feeding");
            doThrow(new EventValidationException("amountGrams must be a positive number"))
                .when(eventValidator).validateFeeding(invalidData);

            assertThrows(EventValidationException.class,
                () -> eventService.createFeeding(PET_ID, EMAIL, request));

            verify(eventValidator).validateFeeding(invalidData);
            verifyNoInteractions(eventRepository);
        }

        @Test
        void shouldCreateNoteSuccessfully() {
            allowCreation();
            CreateNoteRequest request = new CreateNoteRequest(OCCURRED_AT, "Vet visit", "Annual checkup");
            Map<String, Object> expectedData = Map.of("title", "Vet visit", "description", "Annual checkup");
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createNote(PET_ID, EMAIL, request);

            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.NOTE, result.getType());
            assertEquals(OCCURRED_AT, result.getOccurredAt());
            assertEquals(expectedData, result.getData());
            verify(eventValidator).validateNote(expectedData);
            verify(eventRepository).save(result);
        }

        @Test
        void shouldCreateWalkSuccessfully() {
            allowCreation();
            Long participantId = 2L;
            User participant = mock(User.class);
            OffsetDateTime endTime = OCCURRED_AT.plusMinutes(30);
            CreateWalkRequest request = new CreateWalkRequest(OCCURRED_AT, endTime, participantId, "Park walk");
            Map<String, Object> expectedData = Map.of(
                "startTime", OCCURRED_AT.toString(), "endTime", endTime.toString(),
                "participantId", participantId, "comment", "Park walk", "status", "FINISHED"
            );
            when(userService.getById(participantId)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(true);
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createWalk(PET_ID, EMAIL, request);

            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.WALK, result.getType());
            assertEquals(OCCURRED_AT, result.getOccurredAt());
            assertEquals(expectedData, result.getData());
            verify(petMemberService).hasAccess(participant, pet);
            verify(eventValidator).validateWalk(expectedData);
            verify(eventRepository).save(result);
        }

        @Test
        void shouldRejectWalkWithInvalidParticipant() {
            allowCreation();
            Long participantId = 2L;
            User participant = mock(User.class);
            CreateWalkRequest request = new CreateWalkRequest(
                OCCURRED_AT, OCCURRED_AT.plusMinutes(30), participantId, "Park walk"
            );
            when(userService.getById(participantId)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(false);

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.createWalk(PET_ID, EMAIL, request));

            assertEquals("Participant is not a member of this pet", exception.getMessage());
            verify(petMemberService).hasAccess(participant, pet);
            verifyNoInteractions(eventRepository);
        }
    }

    @Nested
    @DisplayName("Обновление событий")
    class Updating {

        @Test
        void shouldUpdateEventWhenOwnerIsNotAuthor() {
            configureEventAccess(true);
            configureRole(Role.OWNER);
            User author = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(author.getId()).thenReturn(2L);
            Event event = new Event(author, pet, EventType.FEEDING, OCCURRED_AT,
                new HashMap<>(Map.of("amountGrams", 80)));
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            OffsetDateTime newOccurredAt = OCCURRED_AT.plusHours(1);
            Map<String, Object> expectedData = Map.of("amountGrams", 100);

            EventResponse response = eventService.update(PET_ID, EVENT_ID, EMAIL,
                new UpdateEventRequest(newOccurredAt, expectedData));

            assertEquals(expectedData, response.data());
            assertEquals(expectedData, event.getData());
            assertEquals(newOccurredAt, response.occurredAt());
            assertEquals(newOccurredAt, event.getOccurredAt());
            assertSame(author, event.getCreatedBy());
            verify(eventValidator).validateFeeding(expectedData);
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldUpdateEventWhenMemberIsAuthor() {
            configureEventAccess(true);
            configureRole(Role.MEMBER);
            when(user.getId()).thenReturn(1L);
            Event event = feedingEvent();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            OffsetDateTime newOccurredAt = OCCURRED_AT.plusHours(1);
            Map<String, Object> expectedData = Map.of("amountGrams", 100, "comment", "Morning feeding");

            EventResponse response = eventService.update(PET_ID, EVENT_ID, EMAIL,
                new UpdateEventRequest(newOccurredAt, Map.of("amountGrams", 100)));

            assertEquals(expectedData, response.data());
            assertEquals(expectedData, event.getData());
            assertEquals(newOccurredAt, response.occurredAt());
            assertEquals(newOccurredAt, event.getOccurredAt());
            verify(eventValidator).validateFeeding(expectedData);
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotUpdateEventWhenMemberIsNotAuthor() {
            configureEventAccess(true);
            configureRole(Role.MEMBER);
            User author = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(author.getId()).thenReturn(2L);
            Event event = new Event(author, pet, EventType.FEEDING, OCCURRED_AT,
                new HashMap<>(Map.of("amountGrams", 80)));
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(OCCURRED_AT.plusHours(1), Map.of("amountGrams", 100));

            assertThrows(AccessDeniedException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals(originalData, event.getData());
            assertEquals(OCCURRED_AT, event.getOccurredAt());
            verifyNoInteractions(eventValidator);
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotUpdateEventWhenViewer() {
            configureEventAccess(true);
            configureRole(Role.VIEWER);
            when(user.getId()).thenReturn(1L);
            Event event = feedingEvent();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(OCCURRED_AT.plusHours(1), Map.of("amountGrams", 100));

            assertThrows(AccessDeniedException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals(originalData, event.getData());
            assertEquals(OCCURRED_AT, event.getOccurredAt());
            verifyNoInteractions(eventValidator);
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldUpdateFeedingSuccessfully() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of("amountGrams", 100));
            Map<String, Object> expectedData = Map.of("amountGrams", 100, "comment", "Morning feeding");

            EventResponse response = eventService.update(PET_ID, EVENT_ID, EMAIL, request);

            assertEquals(expectedData, response.data());
            assertEquals(expectedData, event.getData());
            assertEquals(EventType.FEEDING, response.type());
            assertEquals(OCCURRED_AT, response.occurredAt());
            verify(eventValidator).validateFeeding(expectedData);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldUpdateOccurredAtSuccessfully() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            OffsetDateTime newOccurredAt = OCCURRED_AT.plusHours(1);

            EventResponse response = eventService.update(PET_ID, EVENT_ID, EMAIL,
                new UpdateEventRequest(newOccurredAt, null));

            assertEquals(newOccurredAt, response.occurredAt());
            assertEquals(newOccurredAt, event.getOccurredAt());
            assertEquals(originalData, event.getData());
            verifyNoInteractions(eventValidator);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldNotChangeOccurredAtWhenFeedingValidationFails() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            Map<String, Object> invalidData = Map.of("amountGrams", -50, "comment", "Morning feeding");
            doThrow(new EventValidationException("amountGrams must be a positive number"))
                .when(eventValidator).validateFeeding(invalidData);
            UpdateEventRequest request = new UpdateEventRequest(OCCURRED_AT.plusHours(1), Map.of("amountGrams", -50));

            assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals(OCCURRED_AT, event.getOccurredAt());
            assertEquals(originalData, event.getData());
            verify(eventValidator).validateFeeding(invalidData);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectWalkUpdateWithInvalidParticipant() {
            configureEventAccess(true);
            allowEventModification();
            Long participantId = 2L;
            User participant = mock(User.class);
            Map<String, Object> originalData = Map.of(
                "startTime", OCCURRED_AT.toString(),
                "endTime", OCCURRED_AT.plusMinutes(30).toString(),
                "comment", "Park walk",
                "status", "FINISHED"
            );
            Event event = new Event(user, pet, EventType.WALK, OCCURRED_AT, new HashMap<>(originalData));
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            when(userService.getById(participantId)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(false);
            Map<String, Object> expectedData = new HashMap<>(originalData);
            expectedData.put("participantId", participantId);
            UpdateEventRequest request = new UpdateEventRequest(
                null, Map.of("participantId", participantId)
            );

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals("Participant is not a member of this pet", exception.getMessage());
            assertEquals(OCCURRED_AT, event.getOccurredAt());
            assertEquals(originalData, event.getData());
            verify(eventValidator).validateWalk(expectedData);
            verify(petMemberService).hasAccess(participant, pet);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectInvalidFeedingUpdate() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            Map<String, Object> invalidData = Map.of("amountGrams", -50, "comment", "Morning feeding");
            doThrow(new EventValidationException("amountGrams must be a positive number"))
                .when(eventValidator).validateFeeding(invalidData);
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of("amountGrams", -50));

            assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals(originalData, event.getData());
            assertEquals(OCCURRED_AT, event.getOccurredAt());
            verify(eventValidator).validateFeeding(invalidData);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldNotUpdateNonExistingEvent() {
            configureEventAccess(true);
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.empty());
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of("amountGrams", 100));

            assertThrows(EventNotFoundException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
            verifyNoInteractions(eventValidator);
        }

        @Test
        void shouldNotUpdateEventWhenAccessDenied() {
            configureEventAccess(false);
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of("amountGrams", 100));

            assertThrows(AccessDeniedException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            verifyNoInteractions(eventRepository, eventValidator);
        }

        @Test
        void shouldRejectOccurredAtUpdateForActiveWalk() {
            configureEventAccess(true);
            allowEventModification();

            Event event = activeWalk();
            OffsetDateTime originalOccurredAt = event.getOccurredAt();

            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            UpdateEventRequest request = new UpdateEventRequest(
                originalOccurredAt.plusMinutes(10),
                null
            );

            EventValidationException exception = assertThrows(
                EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request)
            );

            assertEquals(
                "Walk occurredAt cannot be updated directly",
                exception.getMessage()
            );
            assertEquals(originalOccurredAt, event.getOccurredAt());
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectStatusUpdateForWalk() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            OffsetDateTime originalOccurredAt = event.getOccurredAt();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of("status", "FINISHED"));

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals("Walk status cannot be updated directly", exception.getMessage());
            assertEquals(originalData, event.getData());
            assertEquals(originalOccurredAt, event.getOccurredAt());
            verifyNoInteractions(eventValidator);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectStartTimeUpdateForActiveWalk() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            OffsetDateTime originalOccurredAt = event.getOccurredAt();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(null,
                Map.of("startTime", originalOccurredAt.plusMinutes(10).toString()));

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals("Cannot modify startTime or endTime of an active walk", exception.getMessage());
            assertEquals(originalData, event.getData());
            assertEquals(originalOccurredAt, event.getOccurredAt());
            verifyNoInteractions(eventValidator);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldRejectEndTimeUpdateForActiveWalk() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            OffsetDateTime originalOccurredAt = event.getOccurredAt();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(null,
                Map.of("endTime", originalOccurredAt.plusMinutes(30).toString()));

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals("Cannot modify startTime or endTime of an active walk", exception.getMessage());
            assertEquals(originalData, event.getData());
            assertEquals(originalOccurredAt, event.getOccurredAt());
            verifyNoInteractions(eventValidator);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldUpdateActiveWalkCommentSuccessfully() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            event.getData().put("comment", "Original comment");
            OffsetDateTime originalOccurredAt = event.getOccurredAt();
            Map<String, Object> expectedData = new HashMap<>(event.getData());
            expectedData.put("comment", "Updated comment");
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of("comment", "Updated comment"));

            EventResponse response = eventService.update(PET_ID, EVENT_ID, EMAIL, request);

            assertEquals(expectedData, event.getData());
            assertEquals(expectedData, response.data());
            assertEquals(originalOccurredAt, event.getOccurredAt());
            assertEquals(originalOccurredAt, response.occurredAt());
            verify(eventValidator).validateWalk(expectedData);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldUpdateFinishedWalkTimeSuccessfully() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            event.getData().put("status", "FINISHED");
            event.getData().put("endTime", event.getOccurredAt().plusMinutes(30).toString());
            event.getData().put("comment", "Park walk");
            OffsetDateTime newStartTime = event.getOccurredAt().minusMinutes(10);
            OffsetDateTime newEndTime = newStartTime.plusMinutes(45);
            Map<String, Object> timeUpdates = Map.of(
                "startTime", newStartTime.toString(),
                "endTime", newEndTime.toString()
            );
            Map<String, Object> expectedData = new HashMap<>(event.getData());
            expectedData.putAll(timeUpdates);
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            UpdateEventRequest request = new UpdateEventRequest(null, timeUpdates);

            EventResponse response = eventService.update(PET_ID, EVENT_ID, EMAIL, request);

            assertEquals(expectedData, event.getData());
            assertEquals(expectedData, response.data());
            assertEquals(newStartTime, event.getOccurredAt());
            assertEquals(newStartTime, response.occurredAt());
            assertEquals(event.getOccurredAt(), OffsetDateTime.parse((String) event.getData().get("startTime")));
            verify(eventValidator).validateWalk(expectedData);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void shouldNotModifyWalkTimeWhenParticipantValidationFails() {
            configureEventAccess(true);
            allowEventModification();
            Event event = activeWalk();
            event.getData().put("status", "FINISHED");
            event.getData().put("endTime", event.getOccurredAt().plusMinutes(30).toString());
            OffsetDateTime originalOccurredAt = event.getOccurredAt();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            Long participantId = 2L;
            User participant = mock(User.class);
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            when(userService.getById(participantId)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(false);
            UpdateEventRequest request = new UpdateEventRequest(null, Map.of(
                "startTime", originalOccurredAt.minusMinutes(10).toString(),
                "participantId", participantId
            ));
            Map<String, Object> mergedData = new HashMap<>(originalData);
            mergedData.putAll(request.data());

            EventValidationException exception = assertThrows(EventValidationException.class,
                () -> eventService.update(PET_ID, EVENT_ID, EMAIL, request));

            assertEquals("Participant is not a member of this pet", exception.getMessage());
            assertEquals(originalOccurredAt, event.getOccurredAt());
            assertEquals(originalData, event.getData());
            verify(eventValidator).validateWalk(mergedData);
            verify(petMemberService).hasAccess(participant, pet);
            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("Получение событий")
    class Retrieval {

        @Test
        void shouldGetEventsWhenViewer() {
            allowCreation();
            PageRequest pageable = PageRequest.of(0, 10);
            Event event = feedingEvent();
            Page<Event> expectedPage = new PageImpl<>(List.of(event), pageable, 1);
            when(eventRepository.findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDescIdDesc(PET_ID, pageable))
                .thenReturn(expectedPage);

            Page<Event> result = eventService.getAllByPetId(PET_ID, EMAIL, 0, 10);

            assertSame(expectedPage, result);
            assertEquals(List.of(event), result.getContent());
            assertEquals(1, result.getTotalElements());
            verify(petService).getByIdForUser(PET_ID, user);
            verify(eventRepository).findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDescIdDesc(PET_ID, pageable);
            verifyNoInteractions(petMemberService, eventValidator);
        }

        @Test
        void shouldGetEventByIdSuccessfully() {
            configureEventAccess(true);
            Event event = feedingEvent();
            ReflectionTestUtils.setField(event, "id", EVENT_ID);
            OffsetDateTime createdAt = OCCURRED_AT.plusSeconds(5);
            ReflectionTestUtils.setField(event, "createdAt", createdAt);
            when(pet.getId()).thenReturn(PET_ID);
            when(user.getId()).thenReturn(2L);
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            EventResponse response = eventService.getById(PET_ID, EVENT_ID, EMAIL);

            assertEquals(EVENT_ID, response.id());
            assertEquals(PET_ID, response.petId());
            assertEquals(2L, response.createdBy());
            assertEquals(EventType.FEEDING, response.type());
            assertEquals(OCCURRED_AT, response.occurredAt());
            assertEquals(event.getData(), response.data());
            assertEquals(createdAt, response.createdAt());
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotGetNonExistingEvent() {
            configureEventAccess(true);
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class,
                () -> eventService.getById(PET_ID, EVENT_ID, EMAIL));

            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldGetEventsWithPagination() {
            allowCreation();
            PageRequest pageable = PageRequest.of(1, 2);
            Event event = feedingEvent();
            Page<Event> expectedPage = new PageImpl<>(List.of(event), pageable, 3);
            when(eventRepository.findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDescIdDesc(PET_ID, pageable))
                .thenReturn(expectedPage);

            Page<Event> result = eventService.getAllByPetId(PET_ID, EMAIL, 1, 2);

            assertSame(expectedPage, result);
            assertEquals(List.of(event), result.getContent());
            assertEquals(1, result.getNumber());
            assertEquals(2, result.getSize());
            assertEquals(3, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            verify(petService).getByIdForUser(PET_ID, user);
            verify(eventRepository)
                .findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDescIdDesc(PET_ID, pageable);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotGetEventsWhenAccessDenied() {
            when(userService.getByEmail(EMAIL)).thenReturn(user);
            when(petService.getByIdForUser(PET_ID, user))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> eventService.getAllByPetId(PET_ID, EMAIL, 0, 10));

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
            verifyNoInteractions(eventRepository);
        }
    }

    @Nested
    @DisplayName("Удаление событий")
    class Deletion {

        @Test
        void shouldDeleteEventWhenOwnerIsNotAuthor() {
            configureEventAccess(true);
            configureRole(Role.OWNER);
            User author = mock(User.class);
            when(user.getId()).thenReturn(1L);
            when(author.getId()).thenReturn(2L);
            Event event = new Event(author, pet, EventType.FEEDING, OCCURRED_AT,
                new HashMap<>(Map.of("amountGrams", 80)));
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            eventService.delete(PET_ID, EVENT_ID, EMAIL);

            assertNotNull(event.getDeletedAt());
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotDeleteEventWhenViewer() {
            configureEventAccess(true);
            configureRole(Role.VIEWER);
            when(user.getId()).thenReturn(1L);
            Event event = feedingEvent();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            assertThrows(AccessDeniedException.class,
                () -> eventService.delete(PET_ID, EVENT_ID, EMAIL));

            assertNull(event.getDeletedAt());
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldSoftDeleteEventSuccessfully() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            Map<String, Object> originalData = new HashMap<>(event.getData());
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            assertNull(event.getDeletedAt());
            OffsetDateTime beforeDeletion = OffsetDateTime.now();

            eventService.delete(PET_ID, EVENT_ID, EMAIL);

            OffsetDateTime afterDeletion = OffsetDateTime.now();
            assertNotNull(event.getDeletedAt());
            assertFalse(event.getDeletedAt().isBefore(beforeDeletion));
            assertFalse(event.getDeletedAt().isAfter(afterDeletion));
            assertEquals(originalData, event.getData());
            assertEquals(OCCURRED_AT, event.getOccurredAt());
            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotDeleteNonExistingEvent() {
            configureEventAccess(true);
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class,
                () -> eventService.delete(PET_ID, EVENT_ID, EMAIL));

            verify(eventRepository).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotDeleteEventWhenAccessDenied() {
            configureEventAccess(false);

            assertThrows(AccessDeniedException.class,
                () -> eventService.delete(PET_ID, EVENT_ID, EMAIL));

            verifyNoInteractions(eventRepository);
        }

        @Test
        void shouldNotDeleteAlreadyDeletedEvent() {
            configureEventAccess(true);
            allowEventModification();
            Event event = feedingEvent();
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenAnswer(invocation -> event.getDeletedAt() == null ? Optional.of(event) : Optional.empty());
            eventService.delete(PET_ID, EVENT_ID, EMAIL);
            OffsetDateTime deletedAt = event.getDeletedAt();
            assertNotNull(deletedAt);

            assertThrows(EventNotFoundException.class,
                () -> eventService.delete(PET_ID, EVENT_ID, EMAIL));

            assertEquals(deletedAt, event.getDeletedAt());
            verify(eventRepository, times(2)).findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID);
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldNotDeleteEventWhenMemberIsNotAuthor() {

            configureEventAccess(true);

            User author = mock(User.class);
            Event event = new Event(
                author,
                pet,
                EventType.FEEDING,
                OCCURRED_AT,
                new HashMap<>(Map.of("amountGrams", 80))
            );

            when(petMemberService.hasRole(user, pet, Role.OWNER))
                .thenReturn(false);

            when(petMemberService.hasRole(user, pet, Role.MEMBER))
                .thenReturn(true);

            when(user.getId()).thenReturn(1L);
            when(author.getId()).thenReturn(2L);

            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            assertThrows(
                AccessDeniedException.class,
                () -> eventService.delete(PET_ID, EVENT_ID, EMAIL)
            );

            assertNull(event.getDeletedAt());

            verify(eventRepository, never()).delete(any(Event.class));
        }

        @Test
        void shouldDeleteEventWhenMemberIsAuthor() {

            configureEventAccess(true);

            Event event = feedingEvent();

            when(user.getId()).thenReturn(1L);

            when(petMemberService.hasRole(user, pet, Role.OWNER))
                .thenReturn(false);

            when(petMemberService.hasRole(user, pet, Role.MEMBER))
                .thenReturn(true);

            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));

            eventService.delete(PET_ID, EVENT_ID, EMAIL);

            assertNotNull(event.getDeletedAt());

            verify(eventRepository, never()).delete(any(Event.class));

        }
    }
}
