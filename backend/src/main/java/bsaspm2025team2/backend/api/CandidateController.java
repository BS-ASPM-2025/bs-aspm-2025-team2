package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.CandidateCardResponse;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.repository.CandidateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
