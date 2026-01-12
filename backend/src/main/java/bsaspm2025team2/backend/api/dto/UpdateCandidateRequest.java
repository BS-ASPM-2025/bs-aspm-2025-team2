package bsaspm2025team2.backend.api.dto;

public record UpdateCandidateRequest(
        String fullName,
        String email,
        String phone,
        String skills,
        Integer yearsOfExperience
) {}
