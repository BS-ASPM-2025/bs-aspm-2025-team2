package bsaspm2025team2.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreServiceTest {

    private final ScoreService scoreService = new ScoreService();

    @Test
    void tokenizationLowercasesAndSplitsIntoExactTokens() {
        assertThat(scoreService.tokenize("Java, Spring-Boot; SQL\nDocker"))
                .containsExactlyInAnyOrder("java", "spring", "boot", "sql", "docker");
    }

    @Test
    void scoreIsRoundedBasedOnExactMatches() {
        // required: java, spring, sql, docker (4)
        // candidate: java, sql (2) => 2/4 = 0.5 => 50
        int score = scoreService.score("JAVA, SQL", "java spring sql docker");
        assertThat(score).isEqualTo(50);
    }

    @Test
    void scoreIsCaseInsensitive() {
        int score = scoreService.score("Java", "java");
        assertThat(score).isEqualTo(100);
    }

    @Test
    void requiredCountZeroGivesScoreZero() {
        assertThat(scoreService.score("java sql", null)).isEqualTo(0);
        assertThat(scoreService.score("java sql", "")).isEqualTo(0);
        assertThat(scoreService.score("java sql", "   ")).isEqualTo(0);
    }

    @Test
    void nullOrBlankCandidateSkillsGivesScoreZero() {
        assertThat(scoreService.score(null, "java")).isEqualTo(0);
        assertThat(scoreService.score("", "java")).isEqualTo(0);
        assertThat(scoreService.score("   ", "java")).isEqualTo(0);
    }

    @Test
    void exactMatchOnlyNoSubstrings() {
        // candidate has "javascript" but required is "java" -> should not match
        int score = scoreService.score("javascript", "java");
        assertThat(score).isEqualTo(0);
    }
}
