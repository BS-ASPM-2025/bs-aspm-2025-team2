package bsaspm2025team2.backend.api.dto;

public record PositionResponse(
        Long id,
        String name,
        String requiredSkills,
        int skillsWeight,
        int experienceWeight
) {}
