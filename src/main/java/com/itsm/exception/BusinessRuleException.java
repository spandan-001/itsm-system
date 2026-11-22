package com.itsm.exception;

/** A request that breaks a business rule not covered by a more specific exception (HTTP 409). */
public class BusinessRuleException extends ItsmException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
