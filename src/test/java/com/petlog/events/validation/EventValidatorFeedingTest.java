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

class EventValidatorFeedingTest {

    private final EventValidator validator = new EventValidator();

    @Test
    void shouldAcceptFeedingWithoutAmount() {

        Map<String, Object> data = Map.of();

        assertDoesNotThrow(() -> validator.validateFeeding(data));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 50.0, 150.5})
    void shouldAcceptPositiveAmount(double amountGrams) {

        Map<String, Object> data = Map.of(
            "amountGrams", amountGrams
        );

        assertDoesNotThrow(() -> validator.validateFeeding(data));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -50.5})
    void shouldRejectNonPositiveAmount(double amountGrams) {

        Map<String, Object> data = Map.of(
            "amountGrams", amountGrams
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateFeeding(data)
        );

        assertEquals("amountGrams must be a positive number", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"50", "abc", ""})
    void shouldRejectNonNumericAmount(String amountGrams) {

        Map<String, Object> data = Map.of(
            "amountGrams", amountGrams
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateFeeding(data)
        );

        assertEquals("amountGrams must be a positive number", exception.getMessage());
    }

    @Test
    void shouldAcceptNullAmount() {

        Map<String, Object> data = new HashMap<>();
        data.put("amountGrams", null);

        assertDoesNotThrow(() -> validator.validateFeeding(data));
    }

    @ParameterizedTest
    @ValueSource(doubles = {
        Double.NaN,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY
    })
    void shouldRejectNonFiniteAmount(double amountGrams) {

        Map<String, Object> data = Map.of(
            "amountGrams", amountGrams
        );

        EventValidationException exception = assertThrows(
            EventValidationException.class,
            () -> validator.validateFeeding(data)
        );

        assertEquals("amountGrams must be a positive number", exception.getMessage());
    }
}
