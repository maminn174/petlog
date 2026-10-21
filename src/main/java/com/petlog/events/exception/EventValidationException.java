package com.petlog.events.exception;

import com.petlog.events.Event;

public class EventValidationException extends RuntimeException {

    public EventValidationException(String message) {
        super(message);
    }
}
