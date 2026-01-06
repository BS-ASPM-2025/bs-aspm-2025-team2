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

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "skills")
    private String skills;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "is_draft", nullable = false)
    private boolean draft = true;

    protected Candidate() {}

    public Candidate(CandidateStatus status, Instant uploadDate) {
        this.status = status;
        this.uploadDate = uploadDate;
        this.draft = true;
    }

    public Long getId() { return id; }
    public CandidateStatus getStatus() { return status; }
    public Instant getUploadDate() { return uploadDate; }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSkills() { return skills; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public boolean isDraft() { return draft; }

    public void setStatus(CandidateStatus status) { this.status = status; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setSkills(String skills) { this.skills = skills; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
    public void setDraft(boolean draft) { this.draft = draft; }
}
