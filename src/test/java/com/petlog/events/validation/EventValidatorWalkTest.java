package com.petlog.events.validation;

import com.petlog.events.exception.EventValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventValidatorWalkTest {

    private final EventValidator validator = new EventValidator();

    @Test
    void shouldAcceptValidWalk() {

        Map<String, Object> data = Map.of(
            "startTime", "2026-10-22T10:00:00+03:00",
            "endTime", "2026-10-22T11:00:00+03:00"
        );

        assertDoesNotThrow(() -> validator.validateWalk(data));
    }

    @Test
    void shouldAcceptWalkWithDifferentUtcOffsets() {

        Map<String, Object> data = Map.of(
            "startTime", "2026-10-22T10:00:00+03:00",
            "endTime", "2026-10-22T10:30:00+02:00"
        );

        assertDoesNotThrow(() -> validator.validateWalk(data));
    }

    @Test
    void shouldRejectEndTimeBeforeStartTime() {

        Map<String, Object> data = Map.of(
            "startTime", "2026-10-22T10:00:00+03:00",
            "endTime",   "2026-10-22T09:30:00+03:00"
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("endTime must be after startTime", exception.getMessage());
    }

    @Test
    void shouldRejectEqualStartAndEndTime() {

        Map<String, Object> data = Map.of(
            "startTime", "2026-10-22T10:00:00+03:00",
            "endTime",   "2026-10-22T10:00:00+03:00"
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("endTime must be after startTime", exception.getMessage());
    }

    @Test
    void shouldRejectMissingStartTime() {

        Map<String, Object> data = Map.of(
            "endTime",   "2026-10-22T10:00:00+03:00"
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("startTime must be a string", exception.getMessage());
    }

    @Test
    void shouldRejectMissingEndTime() {

        Map<String, Object> data = Map.of(
            "startTime",   "2026-10-22T10:00:00+03:00"
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("endTime must be a string", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc",
        "2026-99-99",
        "2026-10-22T10:00:00"
    })
    void shouldRejectInvalidStartTime(String date) {

        Map<String, Object> data = Map.of(
            "startTime", date,
            "endTime",   "2026-10-22T10:00:00+03:00"
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("startTime must be a valid ISO-8601 date-time", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc",
        "2026-99-99",
        "2026-10-22T10:00:00"
    })
    void shouldRejectInvalidEndTime(String date) {

        Map<String, Object> data = Map.of(
            "startTime",   "2026-10-22T10:00:00+03:00",
            "endTime",   date
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("endTime must be a valid ISO-8601 date-time", exception.getMessage());
    }

    @Test
    void shouldRejectNonStringTime() {

        Map<String, Object> data = Map.of(
            "startTime",   "2026-10-22T10:00:00+03:00",
            "endTime",   123
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals("endTime must be a string", exception.getMessage());
    }

    @Test
    void shouldAcceptWalkAcrossMidnight() {

        Map<String, Object> data = Map.of(
            "startTime", "2026-10-22T23:30:00+03:00",
            "endTime", "2026-10-23T00:15:00+03:00"
        );

        assertDoesNotThrow(() -> validator.validateWalk(data));
    }

    @Test
    void shouldRejectEarlierEndTimeAcrossOffsets() {

        Map<String, Object> data = Map.of(
            "startTime", "2026-10-22T10:00:00+02:00",
            "endTime", "2026-10-22T10:30:00+03:00"
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWalk(data)
        );

        assertEquals(
            "endTime must be after startTime",
            exception.getMessage()
        );
    }
}
