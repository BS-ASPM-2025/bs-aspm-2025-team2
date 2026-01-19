package bsaspm2025team2.backend.extraction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CandidateFieldExtractionServiceTest {

    private final CandidateFieldExtractionService service = new CandidateFieldExtractionService();

    @Test
    void extract_whenTextIsBlank_returnsAllNulls() {
        ExtractedCandidateFields f = service.extract("   \n  ");
        assertNull(f.fullName());
        assertNull(f.email());
        assertNull(f.phone());
        assertNull(f.skills());
        assertNull(f.yearsOfExperience());
    }

    @Test
    void extract_extractsEmail_caseInsensitive() {
        String text = "Contact: JOHN.DOE@Example.com";
        ExtractedCandidateFields f = service.extract(text);
        assertEquals("JOHN.DOE@Example.com", f.email());
    }

    @Test
    void extract_extractsAndNormalizesPhone() {
        String text = "Phone: +1 (234) 567-8901";
        ExtractedCandidateFields f = service.extract(text);
        assertEquals("+12345678901", f.phone());
    }

    @Test
    void extract_phoneTooShort_becomesNull() {
        String text = "Phone: 12-34";
        ExtractedCandidateFields f = service.extract(text);
        assertNull(f.phone());
    }

    @Test
    void extract_extractsYearsOfExperience_basic() {
        String text = "I have 5 years of experience in Java.";
        ExtractedCandidateFields f = service.extract(text);
        assertEquals(5, f.yearsOfExperience());
    }

    @Test
    void extract_extractsFullName_fromFirstLines() {
        String text =
                "John Doe\n" +
                        "Software Engineer\n" +
                        "john.doe@mail.com\n" +
                        "+1 234 567 8901\n";
        ExtractedCandidateFields f = service.extract(text);
        assertEquals("John Doe", f.fullName());
    }

    @Test
    void extract_fullName_skipsLinesWithDigitsOrAtSign() {
        String text =
                "John Doe 123\n" +            // digits -> skip
                        "john@doe.com\n" +             // @ -> skip
                        "Jane Mary Smith\n" +          // valid 3 words
                        "Other stuff\n";
        ExtractedCandidateFields f = service.extract(text);
        assertEquals("Jane Mary Smith", f.fullName());
    }

    @Test
    void extract_extractsSkills_keywords_joinedInOrder() {
        String text = "Worked with Java, Spring Boot, Docker and SQL (PostgreSQL).";
        ExtractedCandidateFields f = service.extract(text);

        
        assertNotNull(f.skills());
        assertTrue(f.skills().contains("java"));
        assertTrue(f.skills().contains("spring"));
        assertTrue(f.skills().contains("spring boot"));
        assertTrue(f.skills().contains("docker"));
        assertTrue(f.skills().contains("sql"));
        assertTrue(f.skills().contains("postgresql"));
    }
}
