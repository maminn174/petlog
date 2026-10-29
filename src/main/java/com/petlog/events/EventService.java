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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        petMemberService.requireMemberOrOwner(user, pet);

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

        petMemberService.requireMemberOrOwner(user, pet);

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

        petMemberService.requireMemberOrOwner(user, pet);

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

        petMemberService.requireMemberOrOwner(user, pet);

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
                throw new EventValidationException("Participant is not a member of this pet");
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

        data.put("status", "FINISHED");

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
        User user = userService.getByEmail(email);

        requireCanModifyEvent(user, event);

        if (event.getType() == EventType.WALK
            && request.occurredAt() != null) {
            throw new EventValidationException(
                "Walk occurredAt cannot be updated directly"
            );
        }

        if (request.data() != null) {
            Map<String, Object> currentData = new HashMap<>(event.getData());

            if (event.getType() == EventType.WALK) {
                if (request.data().containsKey("status")) {
                    throw new EventValidationException(
                        "Walk status cannot be updated directly"
                    );
                }

                String status = (String) event.getData().get("status");

                if ("ACTIVE".equals(status)) {
                    if (request.data().containsKey("startTime")
                        || request.data().containsKey("endTime")) {
                        throw new EventValidationException(
                            "Cannot modify startTime or endTime of an active walk"
                        );
                    }
                }
            }

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

            if (event.getType() == EventType.WALK
                && request.data().containsKey("startTime")) {

                OffsetDateTime startTime = OffsetDateTime.parse(
                    (String) currentData.get("startTime")
                );

                event.setOccurredAt(startTime);
            }

            event.setData(currentData);
        }

        if (request.occurredAt() != null) {
            event.setOccurredAt(request.occurredAt());
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
        requireCanModifyEvent(userService.getByEmail(email), event);

        event.setDeletedAt(OffsetDateTime.now());
    }

    @Transactional
    public Event startWalk(
        Long petId,
        String email,
        StartWalkRequest request
    ) {
        User user = userService.getByEmail(email);

        Pet pet = petService.getByIdForUser(petId, user);

        petMemberService.requireMemberOrOwner(user, pet);

        if (eventRepository.findActiveWalkByPetId(petId).isPresent()) {
            throw new ActiveWalkAlreadyExistsException(petId);
        }

        Long participantId = request.participantId() != null
            ? request.participantId()
            : user.getId();

        User participant = userService.getById(participantId);

        if (!petMemberService.hasAccess(participant, pet)) {
            throw new EventValidationException(
                "Participant is not a member of this pet"
            );
        }

        OffsetDateTime startTime = OffsetDateTime.now();

        Map<String, Object> data = new HashMap<>();

        data.put("startTime", startTime.toString());
        data.put("endTime", null);
        data.put("status", "ACTIVE");
        data.put("participantId", participantId);

        Event event = new Event(
            user,
            pet,
            EventType.WALK,
            startTime,
            data
        );

        return eventRepository.save(event);
    }

    @Transactional
    public EventResponse finishWalk(
        Long petId,
        Long eventId,
        String email
    ) {
        Event event = getEventForUser(petId, eventId, email);
        User user = userService.getByEmail(email);
        requireCanModifyEvent(user, event);

        if (event.getType() != EventType.WALK) {
            throw new EventValidationException(
                "Event is not a walk"
            );
        }

        String status = (String) event.getData().get("status");

        if(!"ACTIVE".equals(status)) {
            throw new EventValidationException(
                "Walk is not active"
            );
        }

        Map<String, Object> data = new HashMap<>(event.getData());

        OffsetDateTime endTime = OffsetDateTime.now();

        data.put("endTime", endTime.toString());
        data.put("status", "FINISHED");
        event.setData(data);

        return EventResponse.from(event);
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

    private void requireCanModifyEvent(
        User user,
        Event event
    ) {
        Pet pet = event.getPet();

        boolean isOwner = petMemberService.hasRole(user, pet, Role.OWNER);
        boolean isMember = petMemberService.hasRole(user, pet, Role.MEMBER);
        boolean isAuthor = user.getId().equals(
            event.getCreatedBy().getId()
        );

        if (!(isOwner || (isMember && isAuthor))) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public Optional<Event> getActiveWalk(
        Long petId,
        String email
    ) {
        User user = userService.getByEmail(email);
        petService.getByIdForUser(petId, user);

        return eventRepository.findActiveWalkByPetId(petId);
    }
}
