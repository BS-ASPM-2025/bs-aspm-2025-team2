package bsaspm2025team2.backend.validation;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PdfValidationServiceTest {

    @Test
    void validate_whenFileTooLarge_throwsFileTooLargeException() {
        PdfValidationService service = new PdfValidationService();

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1); // 10MB + 1

        FileTooLargeException ex = assertThrows(FileTooLargeException.class, () -> service.validate(file));
        assertEquals("Maximum allowed file size is 10MB", ex.getMessage());
    }

    @Test
    void validate_whenHeaderIsNotPdf_throwsInvalidFileTypeException() {
        PdfValidationService service = new PdfValidationService();

        byte[] notPdf = "HELLO WORLD".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                notPdf
        );

        InvalidFileTypeException ex = assertThrows(InvalidFileTypeException.class, () -> service.validate(file));
        assertTrue(ex.getMessage().contains("header check failed"));
    }

    @Test
    void validate_whenCannotReadHeader_throwsInvalidFileTypeException() throws Exception {
        PdfValidationService service = new PdfValidationService();

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(100L);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        InvalidFileTypeException ex = assertThrows(InvalidFileTypeException.class, () -> service.validate(file));
        assertEquals("Unable to read file header", ex.getMessage());
    }

    @Test
    void validate_whenMimeTypeIsNotPdf_throwsInvalidFileTypeException() throws Exception {
        PdfValidationService service = new PdfValidationService();


        Tika tika = mock(Tika.class);
        when(tika.detect(any(InputStream.class))).thenReturn("text/plain");
        ReflectionTestUtils.setField(service, "tika", tika);

        byte[] looksLikePdf = minimalPdfBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                looksLikePdf
        );

        InvalidFileTypeException ex = assertThrows(InvalidFileTypeException.class, () -> service.validate(file));
        assertTrue(ex.getMessage().startsWith("Invalid file type: expected application/pdf"));
        assertTrue(ex.getMessage().contains("text/plain"));
    }

    @Test
    void validate_whenValidPdf_passes() {
        PdfValidationService service = new PdfValidationService();

        byte[] pdf = minimalPdfBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ok.pdf",
                "application/pdf",
                pdf
        );

        assertDoesNotThrow(() -> service.validate(file));
    }

    private static byte[] minimalPdfBytes() {
       
        String s = "%PDF-1.4\n" +
                "1 0 obj\n<<>>\nendobj\n" +
                "trailer\n<<>>\n%%EOF";
        return s.getBytes();
    }
}
