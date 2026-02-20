package com.openclassrooms.etudiant.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {IllegalArgumentException.class, IllegalStateException.class})
    protected ResponseEntity<Object> handleConflict(RuntimeException runtimeException, WebRequest request) {
        logError(runtimeException);
        return handleExceptionInternal(runtimeException, getErrorDetails(runtimeException, request), new HttpHeaders(),
                HttpStatus.BAD_REQUEST, request);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
                                                                   @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        logError(ex);
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), message, request.getDescription(false));
        return handleExceptionInternal(ex, errorDetails, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        //return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex, @NonNull HttpHeaders headers,
                                                                   @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String message = "Malformed JSON request or invalid field types";
        logError(ex);
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), message, request.getDescription(false));
        return handleExceptionInternal(ex, errorDetails, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        //return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = {BadCredentialsException.class})
    protected ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException badCredentialsException,
                                                                   WebRequest request) {
        logError(badCredentialsException);
        return handleExceptionInternal(badCredentialsException, getErrorDetails(badCredentialsException, request),
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(value = {AccessDeniedException.class})
    protected ResponseEntity<Object> handleForbiddenException(AccessDeniedException accessDeniedException,
                                                              WebRequest request) {
        logError(accessDeniedException);
        return handleExceptionInternal(accessDeniedException, getErrorDetails(accessDeniedException, request),
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<Object> handleException(RuntimeException runtimeException, WebRequest request) {
        logError(runtimeException);
        return handleExceptionInternal(runtimeException, "Internal Server error", new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private void logError(Exception exception) {
        logger.error(exception.getMessage(), exception);
    }

    private ErrorDetails getErrorDetails(Exception exception, WebRequest request) {
        return new ErrorDetails(LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
    }
}
