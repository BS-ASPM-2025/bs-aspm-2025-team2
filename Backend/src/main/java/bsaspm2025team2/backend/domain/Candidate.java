package bsaspm2025team2.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CandidateStatus status;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate;

    protected Candidate() { }

    public Candidate(CandidateStatus status, Instant uploadDate) {
        this.status = status;
        this.uploadDate = uploadDate;
    }

    public Long getId() { return id; }
    public CandidateStatus getStatus() { return status; }
    public Instant getUploadDate() { return uploadDate; }

    public void setStatus(CandidateStatus status) { this.status = status; }
}
