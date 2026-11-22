package com.itsm.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions from REST controllers into RFC 9457 problem responses.
 * Thymeleaf controllers are not affected.
 */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail invalidTransition(InvalidStateTransitionException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Invalid state transition", ex.getMessage());
        problem.setProperty("from", ex.getFrom());
        problem.setProperty("to", ex.getTo());
        problem.setProperty("allowedTransitions", ex.getAllowed());
        return problem;
    }

    @ExceptionHandler(OperationNotPermittedException.class)
    public ProblemDetail forbidden(OperationNotPermittedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Operation not permitted", ex.getMessage());
    }

    /** Any other business-rule violation. */
    @ExceptionHandler(ItsmException.class)
    public ProblemDetail businessRule(ItsmException ex) {
        return problem(HttpStatus.CONFLICT, "Request conflicts with current state", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
