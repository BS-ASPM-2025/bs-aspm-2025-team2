package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.CandidateListItemResponse;
import bsaspm2025team2.backend.api.dto.PipelineStatsResponse;
import bsaspm2025team2.backend.api.dto.TopCandidatesResponse;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.CandidateRepository;
import bsaspm2025team2.backend.repository.PositionRepository;
import bsaspm2025team2.backend.service.ScoreService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager/reports")
public class ReportsController {

    private final CandidateRepository candidateRepository;
    private final PositionRepository positionRepository;
    private final ScoreService scoreService;

    public ReportsController(CandidateRepository candidateRepository,
                             PositionRepository positionRepository,
                             ScoreService scoreService) {
        this.candidateRepository = candidateRepository;
        this.positionRepository = positionRepository;
        this.scoreService = scoreService;
    }

    /**
     * Report 1: pipeline stats by status, under current filters.
     * Filters: q, status, min_years, min_score, position_id
     */
    @GetMapping("/pipeline-stats")
    public PipelineStatsResponse pipelineStats(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) CandidateStatus status,
            @RequestParam(value = "min_years", required = false) Integer minYears,
            @RequestParam(value = "position_id", required = false) Long positionId,
            @RequestParam(value = "min_score", required = false) Integer minScore
    ) {
        final Position position = positionId == null ? null : positionRepository.findById(positionId).orElse(null);

        List<CandidateWithScore> filtered = filterAndScoreCandidates(q, status, minYears, minScore, position);

        // Initialize all statuses with 0
        Map<String, Long> counts = new LinkedHashMap<>();
        for (CandidateStatus cs : CandidateStatus.values()) {
            counts.put(cs.name(), 0L);
        }

        // Count
        Map<String, Long> actual = filtered.stream()
                .collect(Collectors.groupingBy(cs -> cs.candidate.getStatus().name(), Collectors.counting()));

        actual.forEach(counts::put);

        return new PipelineStatsResponse(counts);
    }

    /**
     * Report 2: top-N candidates by score (under current filters).
     * Supports CSV export: ?download=csv
     */
    @GetMapping("/top-candidates")
    public ResponseEntity<?> topCandidates(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) CandidateStatus status,
            @RequestParam(value = "min_years", required = false) Integer minYears,
            @RequestParam(value = "position_id", required = false) Long positionId,
            @RequestParam(value = "min_score", required = false) Integer minScore,
            @RequestParam(value = "n", required = false, defaultValue = "10") Integer n,
            @RequestParam(value = "download", required = false) String download
    ) {
        int topN = normalizeTopN(n);
        final Position position = positionId == null ? null : positionRepository.findById(positionId).orElse(null);

        List<CandidateWithScore> filtered = filterAndScoreCandidates(q, status, minYears, minScore, position);

        // sort by score desc, tie-breaker upload_date desc
        filtered.sort(Comparator.<CandidateWithScore>comparingInt(cs -> cs.score).reversed()
                .thenComparing(cs -> cs.candidate.getUploadDate(), Comparator.nullsLast(Comparator.reverseOrder())));

        long totalMatched = filtered.size();
        List<CandidateWithScore> top = filtered.stream().limit(topN).toList();

        if ("csv".equalsIgnoreCase(download)) {
            String csv = toCsv(top);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"top-candidates.csv\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv);
        }

        List<CandidateListItemResponse> items = top.stream()
                .map(cs -> toListItem(cs.candidate, cs.score))
                .toList();

        return ResponseEntity.ok(new TopCandidatesResponse(items, topN, totalMatched));
    }

    // ----------------- helpers -----------------

    private List<CandidateWithScore> filterAndScoreCandidates(String q,
                                                              CandidateStatus status,
                                                              Integer minYears,
                                                              Integer minScore,
                                                              Position position) {
        List<Candidate> all = candidateRepository.findAll();

        List<Candidate> filtered = all.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> minYears == null || (c.getYearsOfExperience() != null && c.getYearsOfExperience() >= minYears))
                .filter(c -> matchesQ(c, q))
                .toList();

        List<CandidateWithScore> scored = filtered.stream()
                .map(c -> new CandidateWithScore(c, computeScore(c, position)))
                .collect(Collectors.toList());

        if (minScore != null) {
            int ms = Math.max(0, Math.min(100, minScore));
            scored = scored.stream().filter(cs -> cs.score >= ms).toList();
        }

        return scored;
    }

    private boolean matchesQ(Candidate c, String q) {
        if (q == null || q.isBlank()) return true;
        String needle = q.toLowerCase();

        String fullName = safeLower(c.getFullName());
        String email = safeLower(c.getEmail());
        String phone = safeLower(c.getPhone());

        return fullName.contains(needle) || email.contains(needle) || phone.contains(needle);
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    private int computeScore(Candidate c, Position position) {
        if (position == null) return 0;
        return scoreService.score(c.getSkills(), position.getRequiredSkills());
    }

    private CandidateListItemResponse toListItem(Candidate c, int score) {
        return new CandidateListItemResponse(
                c.getId(),
                c.getFullName(),
                c.getEmail(),
                c.getPhone(),
                c.getStatus() == null ? null : c.getStatus().name(),
                c.getYearsOfExperience(),
                c.getUploadDate(),
                score
        );
    }

    private int normalizeTopN(Integer n) {
        int v = (n == null) ? 10 : n;
        if (v < 1) v = 1;
        if (v > 100) v = 100;
        return v;
    }

    private String toCsv(List<CandidateWithScore> top) {
        StringBuilder sb = new StringBuilder();
        // header
        sb.append("full_name,email,phone,status,score\n");

        for (CandidateWithScore cs : top) {
            Candidate c = cs.candidate;
            sb.append(csv(c.getFullName())).append(',')
                    .append(csv(c.getEmail())).append(',')
                    .append(csv(c.getPhone())).append(',')
                    .append(csv(c.getStatus() == null ? "" : c.getStatus().name())).append(',')
                    .append(cs.score)
                    .append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value;
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        v = v.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }

    private static class CandidateWithScore {
        final Candidate candidate;
        final int score;

        CandidateWithScore(Candidate candidate, int score) {
            this.candidate = candidate;
            this.score = score;
        }
    }
}
