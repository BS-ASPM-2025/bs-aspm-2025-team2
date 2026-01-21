package bsaspm2025team2.backend.extraction;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    // skill library (MVP) keyword matching is word-boundary safe
    private static final List<String> SKILL_KEYWORDS = List.of(
            "java", "spring", "spring boot", "hibernate", "jpa",
            "postgresql", "sql", "docker", "kubernetes",
            "git", "maven", "rest", "microservices", "flyway"
    );

    // Bullets: • (U+2022), ● (U+25CF), ▪ (U+25AA), plus -, – , —
    private static final Pattern BULLET_PREFIX =
            Pattern.compile("^[\\s\\u2022\\u25CF\\u25AA\\-*–—]+"); // • ● ▪ - – —

    // Simple header-like detection (not perfect, but good enough for MVP)
    private static final Pattern SECTION_HEADER =
            Pattern.compile("^\\s*([A-Z][A-Za-z ]{2,})\\s*:?\\s*$");

    private static final Set<String> STOP_HEADERS = new HashSet<>(Arrays.asList(
            "work experience",
            "experience",
            "education",
            "academic projects",
            "projects",
            "relevant courses",
            "courses",
            "certifications",
            "certification",
            "languages",
            "summary",
            "profile",
            "contacts",
            "contact",
            "professional experience"
    ));

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

        // if too short -> null
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

        // fallback: if email found we try line before
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
        if (text == null || text.isBlank()) return null;

        // 1) Skills section
        List<String> fromSection = extractSkillsFromSection(text);
        if (!fromSection.isEmpty()) {
            return String.join(", ", fromSection);
        }

        // 2) Keyword fallback (word boundaries)
        List<String> byKeywords = extractSkillsByKeywords(text, SKILL_KEYWORDS);
        if (!byKeywords.isEmpty()) {
            return String.join(", ", byKeywords);
        }

        // 3) Bullet fallback (first 120 lines)
        List<String> fromBullets = extractSkillsFromBulletLines(text);
        if (!fromBullets.isEmpty()) {
            return String.join(", ", fromBullets);
        }

        return null;
    }

    private List<String> extractSkillsFromSection(String text) {
        List<String> lines = splitLines(text);

        int startIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            String l = normalizeLine(lines.get(i));
            if (l.equalsIgnoreCase("skills") || l.equalsIgnoreCase("skill")) {
                startIdx = i + 1;
                break;
            }
        }
        if (startIdx == -1) return Collections.emptyList();

        List<String> collectedRawLines = new ArrayList<>();

        for (int i = startIdx; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = normalizeLine(raw);

            if (line.isBlank()) {
                // allow a couple empty lines right after header, but stop if we already collected something
                if (!collectedRawLines.isEmpty()) break;
                continue;
            }

            // stop at next known section
            if (isStopHeader(line)) break;

            // if it's a header-like line and matches a known stop header, stop
            if (looksLikeHeader(line) && isStopHeader(line)) break;

            // Sometimes PDFs produce repeated headers; handle "Work Experience:" etc
            if (looksLikeHeader(line) && STOP_HEADERS.contains(stripColon(line).toLowerCase(Locale.ROOT))) break;

            collectedRawLines.add(raw);
        }

        List<String> tokens = collectedRawLines.stream()
                .flatMap(l -> tokenizeSkillLine(l).stream())
                .collect(Collectors.toList());

        return normalizeSkillTokens(tokens);
    }

    private List<String> extractSkillsByKeywords(String text, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return Collections.emptyList();

        String lower = text.toLowerCase(Locale.ROOT);

        // Prefer longer phrases first ("spring boot" before "spring")
        List<String> sorted = new ArrayList<>(keywords);
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));

        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (String kw : sorted) {
            String k = kw.toLowerCase(Locale.ROOT).trim();
            if (k.isEmpty()) continue;

            Pattern p = buildKeywordPattern(k);
            if (p.matcher(lower).find()) {
                found.add(standardizeToken(kw));
            }
        }

        return new ArrayList<>(found);
    }

    private List<String> extractSkillsFromBulletLines(String text) {
        List<String> lines = splitLines(text);
        int limit = Math.min(lines.size(), 120);

        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            String raw = lines.get(i);
            if (raw == null) continue;

            String trimmed = raw.trim();
            if (trimmed.isBlank()) continue;

            String noBullet = BULLET_PREFIX.matcher(trimmed).replaceFirst("").trim();

            // treat as bullet only if prefix was removed
            if (!noBullet.equals(trimmed) && !noBullet.isBlank()) {
                tokens.addAll(tokenizeSkillLine(noBullet));
            }
        }

        return normalizeSkillTokens(tokens);
    }

    /**
     * Turn one raw skill line into tokens:
     * - remove bullet prefix
     * - extract parentheses content as separate tokens
     * - remove parentheses from base line
     * - split by delimiters (comma/;/" & ")
     */
    private List<String> tokenizeSkillLine(String rawLine) {
        if (rawLine == null) return Collections.emptyList();

        String line = rawLine.trim();
        line = BULLET_PREFIX.matcher(line).replaceFirst("").trim();
        if (line.isBlank()) return Collections.emptyList();

        List<String> out = new ArrayList<>();

        // (Salesforce, Zendesk) => separate tokens
        Matcher m = Pattern.compile("\\(([^)]{1,200})\\)").matcher(line);
        while (m.find()) {
            String inside = m.group(1);
            out.addAll(splitByDelimiters(inside));
        }

        // remove parentheses content from base line
        line = line.replaceAll("\\([^)]{0,200}\\)", " ").replaceAll("\\s{2,}", " ").trim();

        out.addAll(splitByDelimiters(line));

        return out;
    }

    private List<String> splitByDelimiters(String s) {
        if (s == null) return Collections.emptyList();

        // split "Git & GitHub"
        String norm = s.replace(" & ", ", ");

        // split by commas/semicolon; also tolerate stray bullets
        String[] parts = norm.split("[,;]+");
        List<String> res = new ArrayList<>();

        for (String p : parts) {
            String t = p.trim();
            if (!t.isBlank()) res.add(t);
        }

        return res;
    }

    private List<String> normalizeSkillTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return Collections.emptyList();

        LinkedHashMap<String, String> canon = new LinkedHashMap<>();
        for (String t : tokens) {
            String cleaned = standardizeToken(t);
            if (cleaned.isBlank()) continue;

            String key = cleaned.toLowerCase(Locale.ROOT);

            // filter trivial noise
            if (key.length() < 2) continue;
            if (key.equals("and")) continue;

            canon.putIfAbsent(key, cleaned);
        }
        return new ArrayList<>(canon.values());
    }

    private String standardizeToken(String token) {
        if (token == null) return "";
        String t = token.trim();
        t = t.replaceAll("\\s{2,}", " ");
        // remove trailing punctuation
        t = t.replaceAll("[\\p{Punct}]+$", "").trim();
        return t;
    }

    private List<String> splitLines(String text) {
        return Arrays.asList(text.split("\\R"));
    }

    private String normalizeLine(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s{2,}", " ");
    }

    private String stripColon(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.endsWith(":")) t = t.substring(0, t.length() - 1).trim();
        return t;
    }

    private boolean looksLikeHeader(String line) {
        if (line == null) return false;
        String l = line.trim();
        if (l.length() < 3 || l.length() > 60) return false;
        return SECTION_HEADER.matcher(l).matches();
    }

    private boolean isStopHeader(String line) {
        String l = stripColon(line).toLowerCase(Locale.ROOT).trim();
        return STOP_HEADERS.contains(l);
    }

    private Pattern buildKeywordPattern(String keywordLower) {
        // If keyword has spaces, still use word boundaries around the whole phrase.
        // Pattern.quote escapes c++ etc.
        String escaped = Pattern.quote(keywordLower);
        return Pattern.compile("\\b" + escaped + "\\b", Pattern.CASE_INSENSITIVE);
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
