package bsaspm2025team2.backend.api.dto;

public record PositionRequest(
        String name,
        String requiredSkills,
        int skillsWeight,
        int experienceWeight
) {}
