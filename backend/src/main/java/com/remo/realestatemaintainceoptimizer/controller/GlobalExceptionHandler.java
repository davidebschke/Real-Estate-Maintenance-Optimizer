package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.ErrorResponse;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into localized HTTP error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AppointmentNotFoundException exception, Locale locale) {
        String message = messageSource.getMessage("appointment.error.notFound", new Object[] {exception.appointmentId()}, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(message));
    }

    @ExceptionHandler(AppointmentLockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(AppointmentLockedException exception, Locale locale) {
        String message = messageSource.getMessage("appointment.error.locked", null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidRecurrenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRecurrence(InvalidRecurrenceException exception, Locale locale) {
        String message = messageSource.getMessage("appointment.error." + exception.reasonCode(), null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(Locale locale) {
        String message = messageSource.getMessage("common.error.validation", null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }
}
