package bsaspm2025team2.backend.validation;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfValidationService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes();

    private final Tika tika = new Tika();

    public void validate(MultipartFile file) {
        validateSize(file);
        validateHeader(file);
        validateMimeType(file);
    }

    private void validateSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException("Maximum allowed file size is 10MB");
        }
    }

    private void validateHeader(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(PDF_MAGIC.length);

            if (header.length < PDF_MAGIC.length ||
                    !startsWith(header, PDF_MAGIC)) {
                throw new InvalidFileTypeException("File is not a valid PDF (header check failed)");
            }
        } catch (IOException e) {
            throw new InvalidFileTypeException("Unable to read file header");
        }
    }

    private void validateMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String mimeType = tika.detect(is);

            if (!"application/pdf".equalsIgnoreCase(mimeType)) {
                throw new InvalidFileTypeException(
                        "Invalid file type: expected application/pdf, got " + mimeType
                );
            }
        } catch (IOException e) {
            throw new InvalidFileTypeException("Unable to detect file MIME type");
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
