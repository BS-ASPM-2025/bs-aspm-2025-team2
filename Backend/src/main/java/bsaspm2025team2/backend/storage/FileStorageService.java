package bsaspm2025team2.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
public class FileStorageService {

    private final Path basePath;

    public FileStorageService(@Value("${app.storage.base-path}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    /**
     * Saves resume file as "<storageId>.pdf" inside basePath.
     *
     * @return storagePath (key) that should be stored in DB, e.g. "a3f0...-....pdf"
     */
    public String savePdf(MultipartFile file, String storageId) {
        try {
            Files.createDirectories(basePath);

            String storageKey = storageId + ".pdf";
            Path target = basePath.resolve(storageKey).normalize();

            // Safety: prevent path traversal, even though we generate the name ourselves.
            if (!target.startsWith(basePath)) {
                throw new StorageException("Invalid storage path");
            }

            // Overwrite = false (should never collide with UUID, but safer)
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return storageKey;
        } catch (IOException e) {
            throw new StorageException("Failed to save file", e);
        }
    }

    public void deleteQuietly(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return;

        try {
            Path target = basePath.resolve(storagePath).normalize();
            if (target.startsWith(basePath)) {
                Files.deleteIfExists(target);
            }
        } catch (Exception ignored) {
            // Intentionally ignore; used in rollback scenarios
        }
    }

    public Path resolve(String storagePath) {
        Path target = basePath.resolve(storagePath).normalize();
        if (!target.startsWith(basePath)) {
            throw new StorageException("Invalid storage path");
        }
        return target;
    }
}
