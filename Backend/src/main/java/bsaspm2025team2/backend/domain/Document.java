package bsaspm2025team2.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    protected Document() { }

    public Document(Candidate candidate,
                    Instant uploadDate,
                    String originalFilename,
                    long fileSize,
                    String contentType,
                    String storagePath) {
        this.candidate = candidate;
        this.uploadDate = uploadDate;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.storagePath = storagePath;
    }

    public Long getId() { return id; }
    public Candidate getCandidate() { return candidate; }
    public Instant getUploadDate() { return uploadDate; }
    public String getOriginalFilename() { return originalFilename; }
    public long getFileSize() { return fileSize; }
    public String getContentType() { return contentType; }
    public String getStoragePath() { return storagePath; }
}
