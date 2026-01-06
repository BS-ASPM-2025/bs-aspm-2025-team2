package bsaspm2025team2.backend.api.dto;

import java.time.Instant;

public record CandidateCardResponse(
        Long candidate_id,
        String status,
        Instant upload_date,
        boolean draft,
        Fields fields,
        Validation validation
) {
    public record Fields(
            String full_name,
            String email,
            String phone,
            String skills,
            Integer years_of_experience
    ) {}

    public record Validation(
            boolean email_required_missing,
            boolean phone_required_missing
    ) {}
}
