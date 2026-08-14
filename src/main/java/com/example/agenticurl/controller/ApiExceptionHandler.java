package com.example.agenticurl.controller;

import com.example.agenticurl.exception.InvalidStateException;
import com.example.agenticurl.exception.NotFoundException;
import com.example.agenticurl.exception.PolicyViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        p.setTitle("Resource not found");
        return p;
    }

    @ExceptionHandler(PolicyViolationException.class)
    ProblemDetail policy(PolicyViolationException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        p.setTitle("Policy violation");
        return p;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        p.setTitle("Invalid request");
        return p;
    }

    @ExceptionHandler(InvalidStateException.class)
    ProblemDetail state(InvalidStateException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        p.setTitle("Invalid state transition");
        return p;
    }
}
