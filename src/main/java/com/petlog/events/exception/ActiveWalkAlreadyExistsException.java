package com.petlog.events.exception;

public class ActiveWalkAlreadyExistsException extends RuntimeException {

    public ActiveWalkAlreadyExistsException(Long petId) {
        super("Pet with id " + petId + " already has active walk");
    }
}
