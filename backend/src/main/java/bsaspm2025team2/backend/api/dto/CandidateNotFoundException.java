package bsaspm2025team2.backend.api.dto;

public class CandidateNotFoundException extends RuntimeException {
    public CandidateNotFoundException(Long id) {
        super("Candidate not found: " + id);
    }
}
