package bsaspm2025team2.backend.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class ScoreService {

    /**
     * MVP tokenization:
     * - lowercase
     * - split into exact tokens (letters/digits), no substrings/synonyms
     * - remove empty tokens
     */
    public Set<String> tokenize(String raw) {
        if (raw == null) return Collections.emptySet();

        String normalized = raw.toLowerCase();
        // split by anything that is not a letter or digit
        String[] parts = normalized.split("[^\\p{L}\\p{Nd}]+");

        Set<String> tokens = new HashSet<>();
        for (String p : parts) {
            if (p != null) {
                String t = p.trim();
                if (!t.isEmpty()) tokens.add(t);
            }
        }
        return tokens;
    }

    /**
     * score = round(100 * matches / required_count)
     * required_count=0 => score=0
     */
    public int score(String candidateSkills, String requiredSkills) {
        Set<String> required = tokenize(requiredSkills);
        if (required.isEmpty()) return 0;

        Set<String> candidate = tokenize(candidateSkills);
        if (candidate.isEmpty()) return 0;

        int matches = 0;
        for (String req : required) {
            if (candidate.contains(req)) matches++;
        }

        double skillMatch = (double) matches / (double) required.size(); // 0..1
        long rounded = Math.round(100.0 * skillMatch);
        if (rounded < 0) return 0;
        if (rounded > 100) return 100;
        return (int) rounded;
    }
}
