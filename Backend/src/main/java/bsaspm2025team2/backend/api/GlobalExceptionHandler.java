package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.ApiErrorResponse;
import bsaspm2025team2.backend.validation.FileTooLargeException;
import bsaspm2025team2.backend.validation.InvalidFileTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
        return new ApiErrorResponse("INTERNAL_ERROR", "Unexpected error");
    }
}
