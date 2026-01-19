package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.CandidateListItemResponse;
import bsaspm2025team2.backend.api.dto.CandidateListResponse;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.CandidateRepository;
import bsaspm2025team2.backend.repository.PositionRepository;
import bsaspm2025team2.backend.service.ScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hr/candidates")
public class CandidateListController {

    private final CandidateRepository candidateRepository;
    private final PositionRepository positionRepository;
    private final ScoreService scoreService;

    public CandidateListController(CandidateRepository candidateRepository,
                                   PositionRepository positionRepository,
                                   ScoreService scoreService) {
        this.candidateRepository = candidateRepository;
        this.positionRepository = positionRepository;
        this.scoreService = scoreService;
    }

    /**
     * US5 params:
     *  q, status, min_years, sort=upload_date_desc (default), limit, offset
     *
     * US7 params:
     *  position_id (optional)
     *  min_score (optional)
     *  sort=score_desc|score_asc|upload_date_desc
     */
    @GetMapping
    public CandidateListResponse listCandidates(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) CandidateStatus status,
            @RequestParam(value = "min_years", required = false) Integer minYears,
            @RequestParam(value = "position_id", required = false) Long positionId,
            @RequestParam(value = "min_score", required = false) Integer minScore,
            @RequestParam(value = "sort", required = false, defaultValue = "upload_date_desc") String sort,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset
    ) {
        int safeLimit = normalizeLimit(limit);
        int safeOffset = Math.max(0, offset);

        final Position position =
                positionId == null
                        ? null
                        : positionRepository.findById(positionId).orElse(null);

        // 1) Fetch ALL candidates (MVP). For small datasets OK.
        List<Candidate> all = candidateRepository.findAll();

        // 2) Apply US5 filters in-memory (MVP).
        List<Candidate> filtered = all.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> minYears == null || (c.getYearsOfExperience() != null && c.getYearsOfExperience() >= minYears))
                .filter(c -> matchesQ(c, q))
                .collect(Collectors.toList());

        // 3) Map to list items with score (relative to selected position)
        List<CandidateWithScore> scored = filtered.stream()
                .map(c -> new CandidateWithScore(c, computeScore(c, position)))
                .collect(Collectors.toList());

        // 4) min_score filter
        if (minScore != null) {
            int ms = Math.max(0, Math.min(100, minScore));
            scored = scored.stream()
                    .filter(cs -> cs.score >= ms)
                    .collect(Collectors.toList());
        }

        // 5) sort
        scored.sort(comparator(sort));

        // 6) paginate AFTER sort/filter
        long total = scored.size();
        int from = Math.min(safeOffset, scored.size());
        int to = Math.min(from + safeLimit, scored.size());
        List<CandidateWithScore> page = scored.subList(from, to);

        List<CandidateListItemResponse> items = page.stream()
                .map(cs -> toItem(cs.candidate, cs.score))
                .collect(Collectors.toList());

        return new CandidateListResponse(items, safeLimit, safeOffset, total);
    }

    private static int normalizeLimit(Integer limit) {
        int l = (limit == null) ? 20 : limit;
        if (l < 1) l = 1;
        if (l > 100) l = 100;
        return l;
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

    private Comparator<CandidateWithScore> comparator(String sort) {
        if ("score_asc".equalsIgnoreCase(sort)) {
            return Comparator.<CandidateWithScore>comparingInt(cs -> cs.score)
                    .thenComparing(cs -> cs.candidate.getUploadDate(), Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("score_desc".equalsIgnoreCase(sort)) {
            return Comparator.<CandidateWithScore>comparingInt((CandidateWithScore cs) -> cs.score).reversed()
                    .thenComparing(cs -> cs.candidate.getUploadDate(), Comparator.nullsLast(Comparator.reverseOrder()));
        }
        // default upload_date_desc
        return Comparator.comparing((CandidateWithScore cs) -> cs.candidate.getUploadDate(),
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private CandidateListItemResponse toItem(Candidate c, int score) {
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

    private static class CandidateWithScore {
        final Candidate candidate;
        final int score;

        CandidateWithScore(Candidate candidate, int score) {
            this.candidate = candidate;
            this.score = score;
        }
    }
}
