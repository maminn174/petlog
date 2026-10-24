package com.petlog.events;

import com.petlog.events.dto.*;
import com.petlog.events.exception.EventNotFoundException;
import com.petlog.events.exception.EventValidationException;
import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.petmember.PetMemberService;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.math.BigDecimal;
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

    @Nested
    @DisplayName("Создание событий")
    class Creation {

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
        void shouldCreateWeightSuccessfully() {
            allowCreation();
            BigDecimal weight = new BigDecimal("5.25");
            CreateWeightRequest request = new CreateWeightRequest(OCCURRED_AT, weight, "Weekly weight");
            Map<String, Object> expectedData = Map.of("weightKg", weight, "comment", "Weekly weight");
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Event result = eventService.createWeight(PET_ID, EMAIL, request);

            assertSame(user, result.getCreatedBy());
            assertSame(pet, result.getPet());
            assertEquals(EventType.WEIGHT, result.getType());
            assertEquals(OCCURRED_AT, result.getOccurredAt());
            assertEquals(expectedData, result.getData());
            verify(eventValidator).validateWeight(expectedData);
            verify(eventRepository).save(result);
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
                "participantId", participantId, "comment", "Park walk"
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
        void shouldUpdateFeedingSuccessfully() {
            configureEventAccess(true);
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
            Long participantId = 2L;
            User participant = mock(User.class);
            Map<String, Object> originalData = Map.of(
                "startTime", OCCURRED_AT.toString(),
                "endTime", OCCURRED_AT.plusMinutes(30).toString(), "comment", "Park walk"
            );
            Event event = new Event(user, pet, EventType.WALK, OCCURRED_AT, new HashMap<>(originalData));
            when(eventRepository.findByDeletedAtIsNullAndIdAndPet_Id(EVENT_ID, PET_ID))
                .thenReturn(Optional.of(event));
            when(userService.getById(participantId)).thenReturn(participant);
            when(petMemberService.hasAccess(participant, pet)).thenReturn(false);
            Map<String, Object> expectedData = new HashMap<>(originalData);
            expectedData.put("participantId", participantId);
            UpdateEventRequest request = new UpdateEventRequest(
                OCCURRED_AT.plusHours(1), Map.of("participantId", participantId)
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
    }

    @Nested
    @DisplayName("Получение событий")
    class Retrieval {

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
        void shouldSoftDeleteEventSuccessfully() {
            configureEventAccess(true);
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
    }
}
