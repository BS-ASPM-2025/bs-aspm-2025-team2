package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.CandidateCardResponse;
import bsaspm2025team2.backend.api.dto.UpdateCandidateRequest;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.repository.CandidateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/candidates")
public class CandidateController {

    private final CandidateRepository candidateRepository;

    public CandidateController(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @GetMapping("/{id}")
    public CandidateCardResponse getCandidateCard(@PathVariable("id") Long id) {
        Candidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));

        return toCardResponse(c);
    }

    @PutMapping("/{id}")
    public CandidateCardResponse updateCandidate(
            @PathVariable("id") Long id,
            @RequestBody UpdateCandidateRequest req
    ) {
        Candidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));

        // Server-side validation: Email + Phone required
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
        return toCardResponse(saved);
    }

    private CandidateCardResponse toCardResponse(Candidate c) {
        boolean emailMissing = (c.getEmail() == null || c.getEmail().isBlank());
        boolean phoneMissing = (c.getPhone() == null || c.getPhone().isBlank());

        return new CandidateCardResponse(
                c.getId(),
                c.getStatus().name(),
                c.getUploadDate(),
                c.isDraft(),
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
