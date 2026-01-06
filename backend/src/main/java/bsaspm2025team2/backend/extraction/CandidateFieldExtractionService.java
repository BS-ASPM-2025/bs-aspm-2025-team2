package bsaspm2025team2.backend.extraction;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CandidateFieldExtractionService {

    // Email
    private static final Pattern EMAIL =
            Pattern.compile("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b");

    // Phone: allow +, space, -, ()
    private static final Pattern PHONE =
            Pattern.compile("(?i)\\b(\\+?\\d[\\d\\s()\\-]{7,}\\d)\\b");

    // Years of experience: "5 years", "5 yrs", "5+ years", "5 years of experience"
    private static final Pattern YEARS =
            Pattern.compile("(?i)\\b(\\d{1,2})\\s*(\\+)?\\s*(years?|yrs?)\\b");

    // skill library (MVP)
    private static final List<String> SKILL_KEYWORDS = List.of(
            "java", "spring", "spring boot", "hibernate", "jpa",
            "postgresql", "sql", "docker", "kubernetes",
            "git", "maven", "rest", "microservices", "flyway"
    );

    public ExtractedCandidateFields extract(String text) {
        if (text == null || text.isBlank()) {
            return new ExtractedCandidateFields(null, null, null, null, null);
        }

        String email = firstMatch(text, EMAIL);
        String phone = normalizePhone(firstMatch(text, PHONE));
        Integer years = extractYears(text);
        String fullName = extractFullName(text, email);
        String skills = extractSkills(text);

        return new ExtractedCandidateFields(fullName, email, phone, skills, years);
    }

    private String firstMatch(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(0).trim() : null;
    }

    private Integer extractYears(String text) {
        Matcher m = YEARS.matcher(text);
        if (!m.find()) return null;

        try {
            int y = Integer.parseInt(m.group(1));
            // sanity: 0..60
            if (y < 0 || y > 60) return null;
            return y;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;

        // leaving only + and digits
        String normalized = raw.replaceAll("[^\\d+]", "");

        // if to short -> null
        if (normalized.replace("+", "").length() < 8) return null;

        return normalized;
    }

    private String extractFullName(String text, String email) {
        // MVP-rule:
        // 1) first 10 lines
        // 2) locating line similar to "First Last" (2-4 words, no digits, without @)
        List<String> lines = firstLines(text, 10);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.contains("@")) continue;
            if (trimmed.matches(".*\\d.*")) continue;

            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2 && parts.length <= 4) {
                // leaving out long headers
                if (trimmed.length() <= 50) {
                    return trimmed;
                }
            }
        }

        // fallback: if email not found we try line before
        if (email != null) {
            int idx = text.indexOf(email);
            if (idx > 0) {
                String before = text.substring(0, idx);
                List<String> prevLines = lastLines(before, 3);
                for (int i = prevLines.size() - 1; i >= 0; i--) {
                    String l = prevLines.get(i).trim();
                    if (!l.isBlank() && !l.contains("@") && !l.matches(".*\\d.*")) {
                        String[] parts = l.split("\\s+");
                        if (parts.length >= 2 && parts.length <= 4 && l.length() <= 50) {
                            return l;
                        }
                    }
                }
            }
        }

        return null;
    }

    private String extractSkills(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        // 1) keyword strategy
        Set<String> found = new LinkedHashSet<>();
        for (String kw : SKILL_KEYWORDS) {
            if (lower.contains(kw)) {
                found.add(kw);
            }
        }

        // 2) return plain text
        if (!found.isEmpty()) {
            return String.join(", ", found);
        }

        // 3) fallback: bullet lines (first 20 lines with '-' or '•')
        List<String> lines = firstLines(text, 50);
        List<String> bullets = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("-") || t.startsWith("•")) {
                bullets.add(t);
            }
        }

        if (!bullets.isEmpty()) {
            return String.join("\n", bullets);
        }

        return null;
    }

    private List<String> firstLines(String text, int n) {
        String[] arr = text.split("\\R");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arr.length && i < n; i++) out.add(arr[i]);
        return out;
    }

    private List<String> lastLines(String text, int n) {
        String[] arr = text.split("\\R");
        List<String> out = new ArrayList<>();
        for (int i = Math.max(0, arr.length - n); i < arr.length; i++) out.add(arr[i]);
        return out;
    }
}
