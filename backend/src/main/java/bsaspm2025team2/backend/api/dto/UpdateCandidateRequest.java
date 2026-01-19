package bsaspm2025team2.backend.api.dto;

import bsaspm2025team2.backend.domain.CandidateStatus;

public record UpdateCandidateRequest(
        CandidateStatus status,
        String fullName,
        String email,
        String phone,
        String skills,
        Integer yearsOfExperience
) {}
