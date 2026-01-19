package bsaspm2025team2.backend.api;

public class CandidateNotFoundException extends RuntimeException {
    public CandidateNotFoundException(Long id) {
        super("Candidate not found: " + id);
    }
}
