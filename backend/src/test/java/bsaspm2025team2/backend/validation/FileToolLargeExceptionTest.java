package bsaspm2025team2.backend.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileTooLargeExceptionTest {

    @Test
    void exceptionStoresProvidedMessage() {
        String message = "File size exceeds maximum limit";

        FileTooLargeException ex = new FileTooLargeException(message);

        assertEquals(message, ex.getMessage());
    }

    @Test
    void exceptionIsRuntimeException() {
        FileTooLargeException ex = new FileTooLargeException("error");

        assertTrue(ex instanceof RuntimeException);
    }
}
