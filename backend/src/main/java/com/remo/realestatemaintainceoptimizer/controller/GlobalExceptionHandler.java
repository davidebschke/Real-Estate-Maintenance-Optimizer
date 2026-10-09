package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.AppointmentConflictResponse;
import com.remo.realestatemaintainceoptimizer.dto.ErrorResponse;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.AiUnavailableException;
import com.remo.realestatemaintainceoptimizer.exception.ApartmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentConflictException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.exception.DemoAccountRestrictedException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidActualEndException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCredentialsException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCurrentPasswordException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidNewPasswordException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalNotPendingException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalOutdatedException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.exception.TenantLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.TenantNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.UsernameAlreadyTakenException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<AppointmentConflictResponse> handleAppointmentConflict(
            AppointmentConflictException exception, Locale locale) {
        String message = messageSource.getMessage("appointment.error.conflict", null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AppointmentConflictResponse(message, exception.suggestedStart(), exception.suggestedEnd()));
    }

    @ExceptionHandler(InvalidRecurrenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRecurrence(InvalidRecurrenceException exception, Locale locale) {
        String message = messageSource.getMessage("appointment.error." + exception.reasonCode(), null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidActualEndException.class)
    public ResponseEntity<ErrorResponse> handleInvalidActualEnd(Locale locale) {
        String message = messageSource.getMessage("appointment.error.actualEndBeforeStart", null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(PropertyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PropertyNotFoundException exception, Locale locale) {
        String message = messageSource.getMessage("property.error.notFound", new Object[] {exception.propertyId()}, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(message));
    }

    @ExceptionHandler(ApartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApartmentNotFound(ApartmentNotFoundException exception, Locale locale) {
        String message = messageSource.getMessage("apartment.error.notFound", new Object[] {exception.apartmentId()}, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(message));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException exception, Locale locale) {
        String message = messageSource.getMessage("tenant.error.notFound", new Object[] {exception.tenantId()}, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(message));
    }

    @ExceptionHandler(TenantLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleTenantLimitExceeded(TenantLimitExceededException exception, Locale locale) {
        String message = messageSource.getMessage("tenant.error." + exception.reasonCode(), new Object[] {exception.limit()}, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(Locale locale) {
        String message = messageSource.getMessage("auth.error.invalidCredentials", null, locale);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(message));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(Locale locale) {
        String message = messageSource.getMessage("auth.error.sessionExpired", null, locale);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(message));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException exception, Locale locale) {
        String message = messageSource.getMessage("auth.error." + exception.reasonCode(), null, locale);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse(message));
    }

    @ExceptionHandler(CreationQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleCreationQuotaExceeded(CreationQuotaExceededException exception, Locale locale) {
        String message = messageSource.getMessage("demo.error." + exception.resource() + "QuotaExceeded", null, locale);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(message));
    }

    @ExceptionHandler(DemoAccountRestrictedException.class)
    public ResponseEntity<ErrorResponse> handleDemoAccountRestricted(Locale locale) {
        String message = messageSource.getMessage("account.error.demoAccountRestricted", null, locale);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(message));
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyTaken(Locale locale) {
        String message = messageSource.getMessage("account.error.usernameTaken", null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(Locale locale) {
        String message = messageSource.getMessage("account.error.currentPasswordIncorrect", null, locale);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidNewPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidNewPassword(InvalidNewPasswordException exception, Locale locale) {
        String message = messageSource.getMessage("account.error." + exception.reasonCode(), null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(RoutingDisabledException.class)
    public ResponseEntity<ErrorResponse> handleRoutingDisabled(Locale locale) {
        String message = messageSource.getMessage("routing.error.disabled", null, locale);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ErrorResponse(message));
    }

    @ExceptionHandler(RoutingUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleRoutingUnavailable(Locale locale) {
        String message = messageSource.getMessage("routing.error.unavailable", null, locale);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(message));
    }

    @ExceptionHandler(AiDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAiDisabled(Locale locale) {
        String message = messageSource.getMessage("ai.error.disabled", null, locale);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ErrorResponse(message));
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(Locale locale) {
        String message = messageSource.getMessage("ai.error.unavailable", null, locale);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(message));
    }

    @ExceptionHandler(OptimizationProposalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOptimizationProposalNotFound(
            OptimizationProposalNotFoundException exception, Locale locale) {
        String message = messageSource.getMessage(
                "optimization.error.proposalNotFound", new Object[] {exception.proposalId()}, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(message));
    }

    @ExceptionHandler(OptimizationProposalNotPendingException.class)
    public ResponseEntity<ErrorResponse> handleOptimizationProposalNotPending(Locale locale) {
        String message = messageSource.getMessage("optimization.error.proposalNotPending", null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
    }

    @ExceptionHandler(OptimizationProposalOutdatedException.class)
    public ResponseEntity<ErrorResponse> handleOptimizationProposalOutdated(Locale locale) {
        String message = messageSource.getMessage("optimization.error.proposalOutdated", null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConcurrentModification(Locale locale) {
        String message = messageSource.getMessage("common.error.concurrentModification", null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(Locale locale) {
        String message = messageSource.getMessage("common.error.validation", null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }
}
