package com.petlog.events;

import com.petlog.events.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets/{petId}/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/feeding")
    public EventResponse createFeeding(
        @PathVariable Long petId,
        @Valid @RequestBody CreateFeedingRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();

        Event event = eventService.createFeeding(
            petId,
            email,
            request
        );

        return EventResponse.from(event);
    }

    @GetMapping
    public Page<EventResponse> getAllEventsByPetId(
        @PathVariable Long petId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        String email = authentication.getName();
        Page<Event> events = eventService.getAllByPetId(petId, email, page, size);

        return events.map(EventResponse::from);
    }

    @PostMapping("/weight")
    public EventResponse createWeight(
        @PathVariable Long petId,
        @Valid @RequestBody CreateWeightRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();

        Event event = eventService.createWeight(
            petId,
            email,
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

        Event event = eventService.createNote(
            petId,
            email,
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

        Event event = eventService.createWalk(
            petId,
            email,
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
