package bsaspm2025team2.backend.api.dto;

import java.util.List;

public record CandidateListResponse(
        List<CandidateListItemResponse> items,
        int limit,
        int offset,
        long total
) {}
