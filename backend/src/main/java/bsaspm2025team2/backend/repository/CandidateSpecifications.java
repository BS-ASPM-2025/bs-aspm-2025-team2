package bsaspm2025team2.backend.repository;

import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;
import org.springframework.data.jpa.domain.Specification;

public class CandidateSpecifications {

    public static Specification<Candidate> statusEquals(CandidateStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Candidate> minYears(Integer minYears) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), minYears);
    }

    public static Specification<Candidate> searchQ(String q) {
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), like),
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("phone")), like)
        );
    }
}
