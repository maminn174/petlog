package com.petlog.common.exception;

import com.petlog.events.EventController;
import com.petlog.events.EventService;
import com.petlog.events.dto.UpdateEventRequest;
import com.petlog.events.exception.EventValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String EMAIL = "test@example.com";
    private final UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(EMAIL, null);

    @Mock
    private EventService eventService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PATCH", "DELETE"})
    void shouldReturnForbiddenWhenEventAccessDenied(String method) throws Exception {
        AccessDeniedException exception = new AccessDeniedException("Access denied to this pet");
        switch (method) {
            case "GET" -> when(eventService.getById(1L, 10L, EMAIL)).thenThrow(exception);
            case "PATCH" -> when(eventService.update(eq(1L), eq(10L), eq(EMAIL), any(UpdateEventRequest.class)))
                .thenThrow(exception);
            case "DELETE" -> doThrow(exception).when(eventService).delete(1L, 10L, EMAIL);
            default -> throw new IllegalArgumentException(method);
        }

        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/pets/1/events/10")
                .principal(authentication).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Access denied to this pet"));
    }

    @Test
    void shouldReturnForbiddenWhenHistoryAccessDenied() throws Exception {
        when(eventService.getAllByPetId(1L, EMAIL, 0, 20))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/pets/1/events").principal(authentication))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestWhenEventValidationFails() throws Exception {
        when(eventService.update(eq(1L), eq(10L), eq(EMAIL), any(UpdateEventRequest.class)))
            .thenThrow(new EventValidationException("amountGrams must be a positive number"));

        mockMvc.perform(patch("/api/pets/1/events/10").principal(authentication)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"data": {"amountGrams": -50}}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("amountGrams must be a positive number"));
    }

    @Test
    void shouldReturnBadRequestWhenWalkParticipantIsInvalid() throws Exception {
        when(eventService.createWalk(eq(1L), eq(EMAIL), any()))
            .thenThrow(new EventValidationException("Participant is not a member of this pet"));

        mockMvc.perform(post("/api/pets/1/events/walk").principal(authentication)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"startTime": "2026-10-22T10:00:00+03:00",
                     "endTime": "2026-10-22T10:30:00+03:00", "participantId": 2}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Participant is not a member of this pet"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestValidationFails() throws Exception {
        mockMvc.perform(post("/api/pets/1/events/weight").principal(authentication)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"occurredAt": "2026-10-22T10:00:00+03:00", "weightKg": -1}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message", startsWith("weightKg: ")));

        verifyNoInteractions(eventService);
    }
}
