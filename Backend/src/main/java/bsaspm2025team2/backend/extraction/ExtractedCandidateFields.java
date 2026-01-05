package bsaspm2025team2.backend.extraction;

public record ExtractedCandidateFields(
        String fullName,
        String email,
        String phone,
        String skills,
        Integer yearsOfExperience
) { }
