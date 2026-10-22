package com.petlog.events;

import com.petlog.events.dto.*;
import com.petlog.events.exception.EventNotFoundException;
import com.petlog.events.exception.EventValidationException;
import com.petlog.events.validation.EventValidator;
import com.petlog.pet.Pet;
import com.petlog.pet.PetService;
import com.petlog.petmember.PetMemberService;
import com.petlog.user.User;
import com.petlog.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventService {

    private final EventRepository eventRepository;

    private final PetService petService;
    private final UserService userService;
    private final PetMemberService petMemberService;
    private final EventValidator eventValidator;

    public EventService(
        EventRepository eventRepository,
        PetService petService,
        UserService userService,
        PetMemberService petMemberService,
        EventValidator eventValidator
    ) {
        this.eventRepository = eventRepository;
        this.petService = petService;
        this.userService = userService;
        this.petMemberService = petMemberService;
        this.eventValidator = eventValidator;
    }

    public Page<Event> getAllByPetId(
        Long petId,
        String email,
        int page,
        int size
    ) {
        User user = userService.getByEmail(email);

        Pageable pageable = PageRequest.of(page, size);

        petService.getByIdForUser(
            petId,
            user
        );
        return eventRepository
            .findAllByDeletedAtIsNullAndPet_IdOrderByOccurredAtDescIdDesc(petId, pageable);
    }

    @Transactional
    public Event createFeeding(
        Long petId,
        String email,
        CreateFeedingRequest request
    ) {
        User user = userService.getByEmail(email);

        Pet pet = petService.getByIdForUser(
            petId,
            user
        );
        Map<String, Object> data = new HashMap<>();

        data.put(
            "amountGrams",
            request.amountGrams()
        );
        data.put(
            "comment",
            request.comment()
        );

        if (request.appetite() != null) {
            data.put(
                "appetite",
                request.appetite().name()
            );
        }

        eventValidator.validateFeeding(data);

        Event event = new Event(
            user,
            pet,
            EventType.FEEDING,
            request.occurredAt(),
            data
        );

        return eventRepository.save(event);
    }

    @Transactional
    public Event createWeight(
        Long petId,
        String email,
        CreateWeightRequest request
    ) {
        User user = userService.getByEmail(email);

        Pet pet = petService.getByIdForUser(
            petId,
            user
        );
        Map<String, Object> data = new HashMap<>();

        data.put(
            "weightKg",
            request.weightKg()
        );
        data.put(
            "comment",
            request.comment()
        );

        eventValidator.validateWeight(data);

        Event event = new Event(
            user,
            pet,
            EventType.WEIGHT,
            request.occurredAt(),
            data
        );

        return eventRepository.save(event);
    }

    @Transactional
    public Event createNote(
        Long petId,
        String email,
        CreateNoteRequest request
    ) {
        User user = userService.getByEmail(email);

        Pet pet = petService.getByIdForUser(
            petId,
            user
        );
        Map<String, Object> data = new HashMap<>();

        data.put(
            "title",
            request.title()
        );
        data.put(
            "description",
            request.description()
        );

        eventValidator.validateNote(data);

        Event event = new Event(
            user,
            pet,
            EventType.NOTE,
            request.occurredAt(),
            data
        );

        return eventRepository.save(event);
    }

    @Transactional
    public Event createWalk(
        Long petId,
        String email,
        CreateWalkRequest request
    ) {
        User user = userService.getByEmail(email);

        Pet pet = petService.getByIdForUser(
            petId,
            user
        );
        Map<String, Object> data = new HashMap<>();

        data.put(
            "startTime",
            request.startTime()
                .toString()
        );

        data.put(
            "endTime",
            request.endTime()
                .toString()
        );

        if (request.participantId() != null) {
            User participant = userService.getById(request.participantId());

            if (!petMemberService.hasAccess(
                participant,
                pet
            )) {
                throw new IllegalArgumentException("Participant is not a member of this pet");
            }

            data.put(
                "participantId",
                request.participantId()
            );
        }

        data.put(
            "comment",
            request.comment()
        );

        eventValidator.validateWalk(data);

        Event event = new Event(
            user,
            pet,
            EventType.WALK,
            request.startTime(),
            data
        );

        return eventRepository.save(event);
    }

    public EventResponse getById(
        Long petId,
        Long eventId,
        String email
    ) {
        Event event = getEventForUser(petId, eventId, email);

        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse update(
        Long petId,
        Long eventId,
        String email,
        UpdateEventRequest request
    ) {
        Event event = getEventForUser(petId, eventId, email);

        if (request.occurredAt() != null) {
            event.setOccurredAt(request.occurredAt());
        }

        if (request.data() != null) {
            Map<String, Object> currentData = new HashMap<>(event.getData());
            currentData.putAll(request.data());

            switch (event.getType()) {
                case FEEDING -> eventValidator.validateFeeding(currentData);
                case WEIGHT -> eventValidator.validateWeight(currentData);
                case NOTE -> eventValidator.validateNote(currentData);
                case WALK -> {
                    eventValidator.validateWalk(currentData);

                    Object participantId = currentData.get("participantId");

                    if (participantId != null) {
                        long id = ((Number)participantId).longValue();
                        User participantUser = userService.getById(id);

                        if (!petMemberService.hasAccess(participantUser, event.getPet())) {
                            throw new EventValidationException(
                                "Participant is not a member of this pet"
                            );
                        }
                    }
                }
            }

            event.setData(currentData);
        }

        return EventResponse.from(event);
    }

    @Transactional
    public void delete(
        Long petId,
        Long eventId,
        String email
    ) {
        Event event = getEventForUser(petId, eventId, email);

        event.setDeletedAt(OffsetDateTime.now());
    }

    private Event getEventForUser(
        Long petId,
        Long eventId,
        String email
    ) {
        User user = userService.getByEmail(email);
        Pet pet = petService.getById(petId);

        if (!petMemberService.hasAccess(user, pet)) {
            throw new AccessDeniedException("Access denied to this pet");
        }

        return eventRepository
            .findByDeletedAtIsNullAndIdAndPet_Id(eventId, petId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
