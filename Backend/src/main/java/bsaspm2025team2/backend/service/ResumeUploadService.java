package bsaspm2025team2.backend.service;

import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;
import bsaspm2025team2.backend.domain.Document;
import bsaspm2025team2.backend.repository.CandidateRepository;
import bsaspm2025team2.backend.repository.DocumentRepository;
import bsaspm2025team2.backend.storage.FileStorageService;
import bsaspm2025team2.backend.validation.PdfValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
public class ResumeUploadService {

    private final PdfValidationService pdfValidationService;
    private final FileStorageService fileStorageService;
    private final CandidateRepository candidateRepository;
    private final DocumentRepository documentRepository;

    public ResumeUploadService(
            PdfValidationService pdfValidationService,
            FileStorageService fileStorageService,
            CandidateRepository candidateRepository,
            DocumentRepository documentRepository
    ) {
        this.pdfValidationService = pdfValidationService;
        this.fileStorageService = fileStorageService;
        this.candidateRepository = candidateRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public Long uploadResume(MultipartFile file) {

        // 1. Validate PDF (size + header + MIME)
        pdfValidationService.validate(file);

        String storagePath = null;

        try {
            // 2. Save file to storage
            String storageId = UUID.randomUUID().toString();
            storagePath = fileStorageService.savePdf(file, storageId);

            // 3. Create Candidate
            Candidate candidate = new Candidate(
                    CandidateStatus.NEW,
                    Instant.now()
            );
            candidate = candidateRepository.save(candidate);

            // 4. Create Document
            Document document = new Document(
                    candidate,
                    Instant.now(),
                    file.getOriginalFilename(),
                    file.getSize(),
                    "application/pdf",
                    storagePath
            );
            documentRepository.save(document);

            // 5. Return candidate id
            return candidate.getId();

        } catch (RuntimeException ex) {
            // rollback storage on any failure
            fileStorageService.deleteQuietly(storagePath);
            throw ex;
        }
    }
}
