package bsaspm2025team2.backend.service;

import bsaspm2025team2.backend.api.CandidateNotFoundException;
import bsaspm2025team2.backend.validation.ValidationException;
import bsaspm2025team2.backend.api.dto.UpdateCandidateRequest;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public Candidate updateCandidate(Long id, UpdateCandidateRequest req) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));

        // server-side required-field validation
        Map<String, String> errors = new LinkedHashMap<>();
        if (req.email() == null || req.email().isBlank()) {
            errors.put("email", "Email is required");
        }
        if (req.phone() == null || req.phone().isBlank()) {
            errors.put("phone", "Phone is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // update fields
        candidate.setFullName(req.fullName());
        candidate.setEmail(req.email());
        candidate.setPhone(req.phone());
        candidate.setSkills(req.skills());
        candidate.setYearsOfExperience(req.yearsOfExperience());

        return candidateRepository.save(candidate);
    }
}
