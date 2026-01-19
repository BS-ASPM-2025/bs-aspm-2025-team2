package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.CandidateCardResponse;
import bsaspm2025team2.backend.api.dto.UpdateCandidateRequest;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.CandidateRepository;
import bsaspm2025team2.backend.repository.PositionRepository;
import bsaspm2025team2.backend.service.ResumeUploadService;
import bsaspm2025team2.backend.service.ScoreService;
import bsaspm2025team2.backend.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/candidates")
public class CandidateController {

    private final CandidateRepository candidateRepository;
    private final PositionRepository positionRepository;
    private final ScoreService scoreService;
    private final ResumeUploadService resumeUploadService;

    public CandidateController(CandidateRepository candidateRepository,
                               ResumeUploadService resumeUploadService,
                               PositionRepository positionRepository,
                               ScoreService scoreService) {
        this.candidateRepository = candidateRepository;
        this.resumeUploadService = resumeUploadService;
        this.positionRepository = positionRepository;
        this.scoreService = scoreService;
    }

    @GetMapping("/{id}")
    public CandidateCardResponse getCandidateCard(
            @PathVariable("id") Long id,
            @RequestParam(value = "position_id", required = false) Long positionId
    ) {
        Candidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));

        return toCardResponse(c, positionId);
    }

    @PutMapping("/{id}")
    public CandidateCardResponse updateCandidate(
            @PathVariable("id") Long id,
            @RequestBody UpdateCandidateRequest req,
            @RequestParam(value = "position_id", required = false) Long positionId
    ) {
        Candidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));

        // Server-side validation: Status + Email + Phone required
        Map<String, String> errors = new LinkedHashMap<>();
        if (req.status() == null) errors.put("status", "Status is required");
        if (req.email() == null || req.email().isBlank()) errors.put("email", "Email is required");
        if (req.phone() == null || req.phone().isBlank()) errors.put("phone", "Phone is required");

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Apply updates
        c.setFullName(req.fullName());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        c.setSkills(req.skills());
        c.setYearsOfExperience(req.yearsOfExperience());
        c.setStatus(req.status());

        // Important: after "Save" data becomes final
        c.setDraft(false);

        Candidate saved = candidateRepository.save(c);
        return toCardResponse(saved, positionId);
    }

    private CandidateCardResponse toCardResponse(Candidate c, Long positionId) {
        boolean emailMissing = (c.getEmail() == null || c.getEmail().isBlank());
        boolean phoneMissing = (c.getPhone() == null || c.getPhone().isBlank());

        int score = 0;
        if (positionId != null) {
            Position position = positionRepository.findById(positionId).orElse(null);
            if (position != null) {
                score = scoreService.score(c.getSkills(), position.getRequiredSkills());
            }
        }

        return new CandidateCardResponse(
                c.getId(),
                c.getStatus().name(),
                c.getUploadDate(),
                c.isDraft(),
                score,
                new CandidateCardResponse.Fields(
                        c.getFullName(),
                        c.getEmail(),
                        c.getPhone(),
                        c.getSkills(),
                        c.getYearsOfExperience()
                ),
                new CandidateCardResponse.Validation(emailMissing, phoneMissing)
        );
    }
}
