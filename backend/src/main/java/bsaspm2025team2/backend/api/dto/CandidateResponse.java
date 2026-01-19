package bsaspm2025team2.backend.api.dto;

import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;

import java.time.Instant;

public record CandidateResponse(
        Long id,
        CandidateStatus status,
        Instant uploadDate,
        String fullName,
        String email,
        String phone,
        String skills,
        Integer yearsOfExperience
) {
    public static CandidateResponse from(Candidate c) {
        return new CandidateResponse(
                c.getId(),
                c.getStatus(),
                c.getUploadDate(),
                c.getFullName(),
                c.getEmail(),
                c.getPhone(),
                c.getSkills(),
                c.getYearsOfExperience()
        );
    }
}
