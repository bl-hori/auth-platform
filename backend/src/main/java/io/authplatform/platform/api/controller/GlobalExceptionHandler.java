package io.authplatform.platform.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API controllers.
 *
 * <p>This handler converts exceptions into RFC 7807 Problem Details
 * for consistent error responses across all API endpoints.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors.
     *
     * <p>Occurs when request body fails @Valid validation.
     *
     * @param ex the validation exception
     * @return problem detail with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://docs.authplatform.io/errors/validation-error"));

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    /**
     * Handle HTTP media type not supported exceptions.
     *
     * <p>Occurs when request Content-Type header is missing or not supported.
     *
     * @param ex the exception
     * @return problem detail
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ProblemDetail handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());

        String detail = ex.getContentType() != null
                ? "Content type '" + ex.getContentType() + "' is not supported"
                : "Content-Type header is required";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                detail
        );
        problemDetail.setTitle("Unsupported Media Type");
        problemDetail.setType(URI.create("https://docs.authplatform.io/errors/unsupported-media-type"));

        if (ex.getSupportedMediaTypes() != null && !ex.getSupportedMediaTypes().isEmpty()) {
            problemDetail.setProperty("supportedMediaTypes", ex.getSupportedMediaTypes().toString());
        }

        return problemDetail;
    }

    /**
     * Handle HTTP message not readable exceptions.
     *
     * <p>Occurs when request body cannot be parsed (invalid JSON, empty body, etc.).
     *
     * @param ex the exception
     * @return problem detail
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request body"
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://docs.authplatform.io/errors/invalid-request-body"));

        return problemDetail;
    }

    /**
     * Handle illegal argument exceptions.
     *
     * <p>Occurs when business logic rejects invalid input.
     *
     * @param ex the exception
     * @return problem detail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://docs.authplatform.io/errors/illegal-argument"));

        return problemDetail;
    }

    /**
     * Handle all other exceptions.
     *
     * <p>Catch-all for unexpected errors.
     *
     * @param ex the exception
     * @return problem detail
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://docs.authplatform.io/errors/internal-error"));

        // Don't expose internal error details in production
        if (log.isDebugEnabled()) {
            problemDetail.setProperty("exception", ex.getClass().getName());
            problemDetail.setProperty("message", ex.getMessage());
        }

        return problemDetail;
    }
}
