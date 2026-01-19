package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.ApiErrorResponse;
import bsaspm2025team2.backend.domain.CandidateStatus;
import bsaspm2025team2.backend.validation.FileTooLargeException;
import bsaspm2025team2.backend.validation.InvalidFileTypeException;
import bsaspm2025team2.backend.validation.ValidationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidFileType(InvalidFileTypeException ex) {
        return new ApiErrorResponse("INVALID_FILE_TYPE", ex.getMessage());
    }

    @ExceptionHandler(FileTooLargeException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiErrorResponse handleFileTooLarge(FileTooLargeException ex) {
        return new ApiErrorResponse("FILE_TOO_LARGE", ex.getMessage());
    }

    // If multipart is rejected by Spring before it reaches our validator
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiErrorResponse handleMaxUpload(MaxUploadSizeExceededException ex) {
        return new ApiErrorResponse("FILE_TOO_LARGE", "Maximum allowed file size is 10MB");
    }

    // Optional: if later you add bean validation to DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        return new ApiErrorResponse("BAD_REQUEST", "Invalid request");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex); // <-- это выведет stacktrace в docker logs
        return new ApiErrorResponse("INTERNAL_ERROR", ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", ex.getMessage(),
                "fields", ex.getErrors()
        ));
    }

    @ExceptionHandler(bsaspm2025team2.backend.api.CandidateNotFoundException.class)
    public ResponseEntity<?> handleNotFound(bsaspm2025team2.backend.api.CandidateNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "NOT_FOUND",
                "message", ex.getMessage()
        ));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && CandidateStatus.class.isAssignableFrom(targetType)) {
                String allowed = Arrays.stream(CandidateStatus.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

                return ResponseEntity.badRequest().body(Map.of(
                        "error", "VALIDATION_ERROR",
                        "message", "Validation failed",
                        "fields", Map.of("status", "Status must be one of: " + allowed)
                ));
            }
        }

        return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", "Malformed JSON request"
        ));
    }

}
