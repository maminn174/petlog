package com.petlog.events.validation;

import com.petlog.events.EventValidator;
import com.petlog.events.exception.EventValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventValidatorNoteTest {

    private final EventValidator validator = new EventValidator();

    @Test
    void shouldRejectMissingTitle() {
        EventValidationException exception = assertThrows(EventValidationException.class,
            () -> validator.validateNote(Map.of()));

        assertEquals("title must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectNonStringTitle() {
        EventValidationException exception = assertThrows(EventValidationException.class,
            () -> validator.validateNote(Map.of("title", 123)));

        assertEquals("title must be a string", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Прогулка",
        "a"
    })
    void shouldAcceptValidNoteTitle(String title) {

        Map<String, Object> data = Map.of(
            "title", title
        );

        assertDoesNotThrow(() -> validator.validateNote(data));
    }

    @Test
    void shouldAcceptNoteWith150CharactersTitle() {

        Map<String, Object> data = Map.of(
            "title", "a".repeat(150)
        );

        assertDoesNotThrow(() -> validator.validateNote(data));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankNoteTitle(String title) {

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateNote(data)
        );

        assertEquals("title must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectNoteWith151CharactersTitle() {

        Map<String, Object> data = Map.of(
            "title", "a".repeat(151)
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateNote(data)
        );

        assertEquals(
            "title length cannot be over 150 characters",
            exception.getMessage()
        );
    }


    @Test
    void shouldAcceptNoteWithoutDescription() {
        Map<String, Object> data = Map.of(
            "title", "Корректный title"
        );

        assertDoesNotThrow(() -> validator.validateNote(data));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldRejectBlankDescription(String description) {

        Map<String, Object> data = Map.of(
            "title", "Корректный title",
            "description", description
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateNote(data)
        );

        assertEquals(
            "description must not be blank",
            exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonStringDescription() {

        Map<String, Object> data = Map.of(
            "title", "Корректный title",
            "description", 123
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateNote(data)
        );

        assertEquals(
            "description must be a string",
            exception.getMessage()
        );
    }
}
