package com.petlog.events.validation;

import com.petlog.events.EventValidator;
import com.petlog.events.exception.EventValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EventValidatorParticipantTest {

    private final EventValidator validator = new EventValidator();

    @Test
    void shouldAcceptNullParticipantId() {

        Object participantId = null;

        assertDoesNotThrow(() -> validator.validateParticipantId(participantId));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100, 1000})
    void shouldAcceptPositiveIntegerId(int id) {

        assertDoesNotThrow(() -> validator.validateParticipantId(id));
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 100L, 1000L})
    void shouldAcceptPositiveLongId(long id) {

        assertDoesNotThrow(() -> validator.validateParticipantId(id));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void shouldRejectNonPositiveId(int id) {

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateParticipantId(id)
        );

        assertEquals("participantId must be positive", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    void shouldRejectNonPositiveLongId(long id) {

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateParticipantId(id)
        );

        assertEquals("participantId must be positive", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "", " "})
    void shouldRejectNonIntegerId(String id) {

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateParticipantId(id)
        );

        assertEquals("participantId must be an integer", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.5, 7.4})
    void shouldRejectDecimalId(Double id) {

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateParticipantId(id)
        );

        assertEquals("participantId must be an integer", exception.getMessage());
    }
}
