package bsaspm2025team2.backend.api.dto;

import java.time.Instant;

public record CandidateListItemResponse(
        Long candidate_id,
        String full_name,
        String email,
        String phone,
        String status,
        Integer years_of_experience,
        Instant upload_date,
        int score
) {}