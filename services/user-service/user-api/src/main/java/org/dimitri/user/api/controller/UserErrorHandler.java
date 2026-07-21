package org.dimitri.user.api.controller;

import org.dimitri.user.application.EmailAlreadyUsedException;
import org.dimitri.user.application.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class UserErrorHandler {
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> notFound(UserNotFoundException exception) {
        return error(exception);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> conflict(EmailAlreadyUsedException exception) {
        return error(exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid(IllegalArgumentException exception) {
        return error(exception);
    }

    private Map<String, String> error(Exception exception) {
        return Map.of("error", exception.getMessage());
    }
}
