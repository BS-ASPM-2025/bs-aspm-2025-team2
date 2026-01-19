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
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportsController.class)
@Import(SecurityConfig.class)
class ReportsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CandidateRepository candidateRepository;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    ScoreService scoreService;

    @Test
    void pipelineStatsAggregatesByStatus() throws Exception {
        // given
        Candidate c1 = mockCandidate(1L, CandidateStatus.NEW);
        Candidate c2 = mockCandidate(2L, CandidateStatus.NEW);
        Candidate c3 = mockCandidate(3L, CandidateStatus.IN_REVIEW);
        Candidate c4 = mockCandidate(4L, CandidateStatus.REJECTED);

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2, c3, c4));

        // when / then
        mockMvc.perform(get("/api/manager/reports/pipeline-stats")
                        .with(httpBasic("manager", "managerPass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.NEW").value(2))
                .andExpect(jsonPath("$.counts.IN_REVIEW").value(1))
                .andExpect(jsonPath("$.counts.REJECTED").value(1))
                .andExpect(jsonPath("$.counts.HIRED").value(0));
    }

    @Test
    void topCandidatesCsvExportWorks() throws Exception {
        // position
        Position position = mock(Position.class);
        when(position.getRequiredSkills()).thenReturn("java sql");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        // candidates
        Candidate c1 = mockCandidateWithSkills(1L, "Alice", "a@mail.com", "111", CandidateStatus.NEW, "java");
        Candidate c2 = mockCandidateWithSkills(2L, "Bob", "b@mail.com", "222", CandidateStatus.IN_REVIEW, "java sql");

        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2));

        when(scoreService.score(eq("java"), anyString())).thenReturn(50);
        when(scoreService.score(eq("java sql"), anyString())).thenReturn(100);

        // when / then
        mockMvc.perform(get("/api/manager/reports/top-candidates")
                        .with(httpBasic("manager", "managerPass"))
                        .param("position_id", "1")
                        .param("download", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"top-candidates.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "full_name,email,phone,status,score")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Bob,b@mail.com,222,IN_REVIEW,100")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Alice,a@mail.com,111,NEW,50")));
    }

    // ---------------- helpers ----------------

    private Candidate mockCandidate(Long id, CandidateStatus status) {
        Candidate c = mock(Candidate.class);
        when(c.getId()).thenReturn(id);
        when(c.getStatus()).thenReturn(status);
        when(c.getUploadDate()).thenReturn(Instant.now());
        when(c.getFullName()).thenReturn("X");
        when(c.getEmail()).thenReturn("x@mail.com");
        when(c.getPhone()).thenReturn("000");
        when(c.getSkills()).thenReturn("java");
        when(c.getYearsOfExperience()).thenReturn(1);
        return c;
    }

    private Candidate mockCandidateWithSkills(Long id,
                                              String name,
                                              String email,
                                              String phone,
                                              CandidateStatus status,
                                              String skills) {
        Candidate c = mock(Candidate.class);
        when(c.getId()).thenReturn(id);
        when(c.getFullName()).thenReturn(name);
        when(c.getEmail()).thenReturn(email);
        when(c.getPhone()).thenReturn(phone);
        when(c.getStatus()).thenReturn(status);
        when(c.getSkills()).thenReturn(skills);
        when(c.getUploadDate()).thenReturn(Instant.now());
        when(c.getYearsOfExperience()).thenReturn(1);
        return c;
    }
}
