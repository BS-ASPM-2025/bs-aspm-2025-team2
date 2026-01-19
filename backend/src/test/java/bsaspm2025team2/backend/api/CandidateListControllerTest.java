package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.config.SecurityConfig;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.CandidateRepository;
import bsaspm2025team2.backend.repository.PositionRepository;
import bsaspm2025team2.backend.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidateListController.class)
@Import(SecurityConfig.class)
class CandidateListControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CandidateRepository candidateRepository;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    ScoreService scoreService;

    @Test
    void listFiltersByMinScore() throws Exception {
        // Position
        Position position = mock(Position.class);
        when(position.getRequiredSkills()).thenReturn("java spring sql docker");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        // Candidates
        Candidate c1 = mockCandidate(1L, "A", "a@mail.com", "111", "JAVA SQL", Instant.parse("2026-01-01T00:00:00Z"));
        Candidate c2 = mockCandidate(2L, "B", "b@mail.com", "222", "java spring sql docker", Instant.parse("2026-01-02T00:00:00Z"));
        Candidate c3 = mockCandidate(3L, "C", "c@mail.com", "333", "python", Instant.parse("2026-01-03T00:00:00Z"));

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        // Scores relative to the selected position
        when(scoreService.score(eq("JAVA SQL"), anyString())).thenReturn(50);
        when(scoreService.score(eq("java spring sql docker"), anyString())).thenReturn(100);
        when(scoreService.score(eq("python"), anyString())).thenReturn(0);

        // min_score=60 -> should keep only c2 (score=100)
        mockMvc.perform(get("/api/hr/candidates")
                        .with(httpBasic("hr", "hrPass"))
                        .param("position_id", "1")
                        .param("min_score", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].candidate_id").value(2))
                .andExpect(jsonPath("$.items[0].score").value(100));
    }

    @Test
    void listSortsByScoreDesc() throws Exception {
        // Position
        Position position = mock(Position.class);
        when(position.getRequiredSkills()).thenReturn("java spring sql docker");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        // Candidates (upload_date tie-breaker not needed here)
        Candidate c1 = mockCandidate(1L, "A", "a@mail.com", "111", "JAVA SQL", Instant.parse("2026-01-01T00:00:00Z"));
        Candidate c2 = mockCandidate(2L, "B", "b@mail.com", "222", "java spring sql docker", Instant.parse("2026-01-02T00:00:00Z"));
        Candidate c3 = mockCandidate(3L, "C", "c@mail.com", "333", "python", Instant.parse("2026-01-03T00:00:00Z"));

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        when(scoreService.score(eq("JAVA SQL"), anyString())).thenReturn(50);
        when(scoreService.score(eq("java spring sql docker"), anyString())).thenReturn(100);
        when(scoreService.score(eq("python"), anyString())).thenReturn(0);

        mockMvc.perform(get("/api/hr/candidates")
                        .with(httpBasic("hr", "hrPass"))
                        .param("position_id", "1")
                        .param("sort", "score_desc")
                        .param("limit", "10")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(3))
                // score_desc: 100, 50, 0
                .andExpect(jsonPath("$.items[0].candidate_id").value(2))
                .andExpect(jsonPath("$.items[0].score").value(100))
                .andExpect(jsonPath("$.items[1].candidate_id").value(1))
                .andExpect(jsonPath("$.items[1].score").value(50))
                .andExpect(jsonPath("$.items[2].candidate_id").value(3))
                .andExpect(jsonPath("$.items[2].score").value(0));
    }

    private Candidate mockCandidate(Long id,
                                    String fullName,
                                    String email,
                                    String phone,
                                    String skills,
                                    Instant uploadDate) {
        Candidate c = mock(Candidate.class);
        when(c.getId()).thenReturn(id);
        when(c.getFullName()).thenReturn(fullName);
        when(c.getEmail()).thenReturn(email);
        when(c.getPhone()).thenReturn(phone);
        when(c.getSkills()).thenReturn(skills);
        when(c.getUploadDate()).thenReturn(uploadDate);
        when(c.getStatus()).thenReturn(CandidateStatus.NEW);
        when(c.getYearsOfExperience()).thenReturn(1);
        return c;
    }
    @Test
    void listFiltersByStatus() throws Exception {
        Candidate c1 = mockCandidate(1L, "A", "a@mail.com", "111", "java", Instant.parse("2026-01-01T00:00:00Z"));
        when(c1.getStatus()).thenReturn(CandidateStatus.NEW);

        Candidate c2 = mockCandidate(2L, "B", "b@mail.com", "222", "java", Instant.parse("2026-01-02T00:00:00Z"));
        when(c2.getStatus()).thenReturn(CandidateStatus.REJECTED);

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/hr/candidates")
                        .with(httpBasic("hr", "hrPass"))
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].candidate_id").value(2))
                .andExpect(jsonPath("$.items[0].status").value("REJECTED"));
    }

    @Test
    void listFiltersByQ_matchesFullNameEmailOrPhone_caseInsensitivePartial() throws Exception {
        Candidate c1 = mockCandidate(1L, "John Doe", "john@doe.com", "555-111", "java", Instant.parse("2026-01-01T00:00:00Z"));
        Candidate c2 = mockCandidate(2L, "Alice Smith", "alice@smith.com", "999-222", "java", Instant.parse("2026-01-02T00:00:00Z"));

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/hr/candidates")
                        .with(httpBasic("hr", "hrPass"))
                        .param("q", "DOE")) // должно найти John Doe и john@doe.com
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].candidate_id").value(1));
    }

    @Test
    void listFiltersByMinYears() throws Exception {
        Candidate c1 = mockCandidate(1L, "A", "a@mail.com", "111", "java", Instant.parse("2026-01-01T00:00:00Z"));
        when(c1.getYearsOfExperience()).thenReturn(1);

        Candidate c2 = mockCandidate(2L, "B", "b@mail.com", "222", "java", Instant.parse("2026-01-02T00:00:00Z"));
        when(c2.getYearsOfExperience()).thenReturn(5);

        Candidate c3 = mockCandidate(3L, "C", "c@mail.com", "333", "java", Instant.parse("2026-01-03T00:00:00Z"));
        when(c3.getYearsOfExperience()).thenReturn(null); // должен отфильтроваться

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        mockMvc.perform(get("/api/hr/candidates")
                        .with(httpBasic("hr", "hrPass"))
                        .param("min_years", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].candidate_id").value(2));
    }

}
