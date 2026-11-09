package mk.finki.roomreservation.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(Map.of("message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            message = "Invalid request parameters.";
        }

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeError(
            DateTimeParseException ex
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid date or time format."
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException ex
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON request body."
        );
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            EmptyResultDataAccessException ex
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "Requested resource was not found."
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleMissingEndpoint(
            NoResourceFoundException ex
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "Requested endpoint was not found."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        log.warn("Database integrity constraint violation", ex);

        return error(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data or database constraints."
        );
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseUnavailable(
            DataAccessResourceFailureException ex
    ) {
        log.error("Database connection or resource failure", ex);

        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Database service is temporarily unavailable."
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseError(
            DataAccessException ex
    ) {
        log.error("Unexpected database operation failure", ex);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal database error occurred."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                "Access denied."
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(
            RuntimeException ex
    ) {
        log.error("Unexpected application error", ex);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred."
        );
    }
}