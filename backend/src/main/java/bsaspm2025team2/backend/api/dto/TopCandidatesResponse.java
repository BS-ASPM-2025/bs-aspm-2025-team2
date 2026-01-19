package bsaspm2025team2.backend.api.dto;

import java.util.List;

public record TopCandidatesResponse(
        List<CandidateListItemResponse> items,
        int n,
        long total_matched
) {}