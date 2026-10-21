package com.petlog.common.exception;

public record ApiError(

    int status,

    String message
) {
}
