package bsaspm2025team2.backend.repository;

import bsaspm2025team2.backend.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
}
