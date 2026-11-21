package com.itsm.exception;

/**
 * Base class for business-rule failures. Domain code throws these without
 * knowing about HTTP; {@link ApiExceptionHandler} maps them to responses.
 */
public abstract class ItsmException extends RuntimeException {

    protected ItsmException(String message) {
        super(message);
    }
}
