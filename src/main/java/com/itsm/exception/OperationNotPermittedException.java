package com.itsm.exception;

/** The signed-in user's role may not perform this action (HTTP 403). */
public class OperationNotPermittedException extends ItsmException {

    public OperationNotPermittedException(String message) {
        super(message);
    }
}
