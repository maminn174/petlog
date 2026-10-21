package com.petlog.events;

import com.petlog.events.dto.*;
import com.petlog.user.User;
import com.petlog.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/events")
public class EventController {

    private final EventService eventService;
    private final UserService userService;
    private final EventRepository eventRepository;

    public EventController(EventService eventService,
                           UserService userService,
                           EventRepository eventRepository
    ) {
        this.eventService = eventService;
        this.userService = userService;
        this.eventRepository = eventRepository;
    }

    @PostMapping("/feeding")
    public EventResponse createFeeding(
        @PathVariable Long petId,
        @Valid @RequestBody CreateFeedingRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);

        Event event = eventService.createFeeding(
            petId,
            user,
            request
        );

        return EventResponse.from(event);
    }

    @GetMapping
    public List<EventResponse> getAllEventsByPetId(
        @PathVariable Long petId,
        Authentication authentication
    ) {
        User user = userService.getByEmail(authentication.getName());
        List<Event> events = eventService.getAllByPetId(petId, user);

        return events.stream()
            .map(EventResponse::from)
            .toList();
    }

    @PostMapping("/weight")
    public EventResponse createWeight(
        @PathVariable Long petId,
        @Valid @RequestBody CreateWeightRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);

        Event event = eventService.createWeight(
            petId,
            user,
            request
        );

        return EventResponse.from(event);
    }

    @PostMapping("/note")
    public EventResponse createNote(
        @PathVariable Long petId,
        @Valid @RequestBody CreateNoteRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);

        Event event = eventService.createNote(
            petId,
            user,
            request
        );

        return EventResponse.from(event);
    }

    @PostMapping("/walk")
    public EventResponse createWalk(
        @PathVariable Long petId,
        @Valid @RequestBody CreateWalkRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userService.getByEmail(email);

        Event event = eventService.createWalk(
            petId,
            user,
            request
        );

        return EventResponse.from(event);
    }

    @GetMapping("/{eventId}")
    public EventResponse getEventById(
        @PathVariable Long petId,
        @PathVariable Long eventId,
        Authentication authentication
    ) {
        String email = authentication.getName();

        return eventService.getById(petId, eventId, email);
    }

    @PatchMapping("/{eventId}")
    public EventResponse update(
        @PathVariable Long petId,
        @PathVariable Long eventId,
        Authentication authentication,
        @RequestBody UpdateEventRequest request
    ) {
        String email = authentication.getName();
        return eventService.update(petId, eventId, email, request);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long petId,
        @PathVariable Long eventId,
        Authentication authentication
    ) {
        String email = authentication.getName();
        eventService.delete(petId, eventId, email);
    }
}
