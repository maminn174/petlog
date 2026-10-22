package com.petlog.events.validation;

import com.petlog.events.exception.EventValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventValidatorWeightTest {

    private final EventValidator validator = new EventValidator();

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 7.4, 25.5})
    void shouldAcceptPositiveWeight(double weight) {

        Map<String, Object> data = Map.of(
            "weightKg", weight
        );

        assertDoesNotThrow(() -> validator.validateWeight(data));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -5.5})
    void shouldRejectNonPositiveWeight(double weight) {

        Map<String, Object> data = Map.of(
            "weightKg", weight
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWeight(data)
        );

        assertEquals("weight must be a positive number", exception.getMessage());
    }

    @Test
    void shouldRejectMissingWeight() {

        Map<String, Object> data = Map.of();

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWeight(data)
        );

        assertEquals("weight cannot be null", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {
        Double.NaN,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY
    })
    void shouldRejectNonFiniteWeight(double weight) {

        Map<String, Object> data = Map.of(
            "weightKg", weight
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWeight(data)
        );

        assertEquals("weight must be a positive number", exception.getMessage());


    }

    @ParameterizedTest
    @ValueSource(strings = {"7.4", "abc", ""})
    void shouldRejectNonNumericWeight(String weight) {

        Map<String, Object> data = Map.of(
            "weightKg", weight
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWeight(data)
        );

        assertEquals("weight must be a positive number", exception.getMessage());
    }

    @Test
    void shouldRejectNullWeight() {

        Map<String, Object> data = new HashMap<>();
        data.put("weightKg", null);

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateWeight(data)
        );

        assertEquals("weight cannot be null", exception.getMessage());
    }
}
