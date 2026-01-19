package bsaspm2025team2.backend.api.dto;

import java.util.Map;

public record PipelineStatsResponse(
        Map<String, Long> counts
) {}