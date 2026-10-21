package com.petlog.events.validation;


import com.petlog.events.exception.EventValidationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Component
public class EventValidator {

    public void validateFeeding(Map<String, Object> data) {

        Object amountGrams = data.get("amountGrams");

        if (amountGrams == null) {
            return;
        }

        if (!(amountGrams instanceof Number number)) {
            throw new EventValidationException(
                "amountGrams must be a positive number"
            );
        }

        if ((number.doubleValue() < 0)) {
            throw new EventValidationException(
                "amountGrams must be a positive number"
            );
        }
    }

    public void validateWeight(Map<String, Object> data) {

        Object weightKg = data.get("weightKg");

        if (weightKg == null) {
            throw new EventValidationException("Weight cannot be null");
        }

        if (!(weightKg instanceof Number number)) {
            throw new EventValidationException(
                "weight must be a positive number"
            );
        }

        if ((number.doubleValue() <= 0)) {
            throw new EventValidationException(
                "weight must be a positive number"
            );
        }
    }

    public void validateNote(Map<String, Object> data) {

        Object description = data.get("description");

        if (description == null) {
            return;
        }

        if (!(description instanceof String string)) {
            throw new EventValidationException(
                "description must be a string"
            );
        }

        if ((string.isBlank())) {
            throw new EventValidationException(
                "description must not be blank"
            );
        }
    }

    public void validateWalk(Map<String, Object> data) {

        Object startTime = data.get("startTime");
        Object endTime = data.get("endTime");

        validateParticipantId(data.get("participantId"));

        OffsetDateTime parsedStartTime = parseDateTime(startTime, "startTime");
        OffsetDateTime parsedEndTime = parseDateTime(endTime, "endTime");

        if (!parsedEndTime.isAfter(parsedStartTime)) {
            throw new EventValidationException(
                "endTime must be after startTime"
            );
        }
    }

    private OffsetDateTime parseDateTime(
        Object value,
        String fieldName
    ) {
        if (!(value instanceof String string)) {
            throw new EventValidationException(
                fieldName + " must be a string");
        }

        try {
            return OffsetDateTime.parse(string);
        } catch (DateTimeParseException e) {
            throw new EventValidationException(
                fieldName + " must be a valid ISO-8601 date-time"
            );
        }
    }

    public void validateParticipantId(Object participantId) {
        if (participantId == null) {
            return;
        }

        if (!(participantId instanceof Integer
            || participantId instanceof Long
        )) {
            throw new EventValidationException(
                "participantId must be an integer"
            );
        }

        long id = ((Number) participantId).longValue();

        if (id <= 0) {
            throw new EventValidationException(
                "participantId must be positive"
            );
        }
    }
}
