package com.smartinstitute.erp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Resource Not Found
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<AllError> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    // 400 - Duplicate / Bad Data
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AllError> handleDatabaseError(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String detail = ex.getMostSpecificCause().getMessage();
        String msg = "Duplicate or invalid data";
        if (detail != null) {
            if (detail.contains("email") || detail.contains("users")) {
                msg = "Duplicate email: a user with this email already exists.";
            } else if (detail.contains("attendance") && (detail.contains("status") || detail.contains("Data truncated"))) {
                msg = "Database rejected attendance status. If you use HOLIDAY, run on MySQL: "
                        + "ALTER TABLE attendance MODIFY COLUMN status VARCHAR(32) NOT NULL;";
            } else if (detail.length() < 300) {
                msg = "Database constraint: " + detail;
            }
        }
        return buildResponse(new RuntimeException(msg), HttpStatus.BAD_REQUEST, request);
    }

    /** Spring returns 400 before controller if a @RequestParam is missing — normalize JSON for the UI. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<AllError> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String msg = "Missing form field \"" + ex.getParameterName()
                + "\". For upload use multipart field names: file, classroomId.";
        return buildResponse(new RuntimeException(msg), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AllError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String msg = "Invalid value for \"" + ex.getName() + "\" (expected "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "number")
                + "). Example: classroomId=1";
        return buildResponse(new RuntimeException(msg), HttpStatus.BAD_REQUEST, request);
    }

    /** JSON body parse errors (wrong types, unknown enum value, bad date). More specific than RuntimeException. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AllError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Throwable root = ex.getMostSpecificCause();
        String msg = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Invalid JSON body. Check classroomId, date (YYYY-MM-DD), and status (PRESENT, ABSENT, HOLIDAY).";
        }
        return buildResponse(new RuntimeException(msg), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<AllError> handleMultipart(Exception ex, HttpServletRequest request) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Upload failed. Check file size (max 10MB) and use field names: file, classroomId.";
        }
        return buildResponse(new RuntimeException(msg), HttpStatus.BAD_REQUEST, request);
    }

    /** Preserves status from ResponseStatusException (not swallowed by the generic Runtime handler). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<AllError> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String reason = ex.getReason();
        Exception forBody = reason != null && !reason.isBlank()
                ? new RuntimeException(reason)
                : ex;
        return buildResponse(forBody, status, request);
    }

    // 400 - Custom Business Errors
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AllError> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // 500 - Unexpected Errors (keep root cause message for debugging; sanitize in production if needed)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AllError> handleAll(
            Exception ex,
            HttpServletRequest request
    ) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = ex.getClass().getSimpleName();
        }
        return buildResponse(
                new RuntimeException(detail),
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<AllError> buildResponse(
            Exception ex,
            HttpStatus status,
            HttpServletRequest request
    ) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = ex.getClass().getSimpleName();
        }
        AllError error = new AllError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                msg,
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, status);
    }
}
