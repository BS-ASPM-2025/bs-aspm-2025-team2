package bsaspm2025team2.backend;

import bsaspm2025team2.backend.api.CandidateController;
import bsaspm2025team2.backend.api.GlobalExceptionHandler;
import bsaspm2025team2.backend.config.SecurityConfig;
import bsaspm2025team2.backend.domain.Candidate;
import bsaspm2025team2.backend.domain.CandidateStatus;
import bsaspm2025team2.backend.repository.CandidateRepository;
import bsaspm2025team2.backend.repository.PositionRepository;
import bsaspm2025team2.backend.service.ResumeUploadService;
import bsaspm2025team2.backend.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidateController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CandidateControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CandidateRepository candidateRepository;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    ScoreService scoreService;

    @MockitoBean
    ResumeUploadService resumeUploadService;

    @Test
    void updateCandidate_missingRequiredFields_returns400_withFieldErrors() throws Exception {
        // Candidate exists
        Candidate c = new Candidate(CandidateStatus.NEW, Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(c, "id", 1L);
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(c));

        String body = """
                {
                  "fullName": "John Doe",
                  "email": "   ",
                  "phone": "",
                  "skills": "java",
                  "yearsOfExperience": 2
                }
                """;

        mockMvc.perform(put("/api/hr/candidates/1")
                        .with(httpBasic("hr", "hrPass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.status").value("Status is required"))
                .andExpect(jsonPath("$.fields.email").value("Email is required"))
                .andExpect(jsonPath("$.fields.phone").value("Phone is required"));
    }

    @Test
    void updateCandidate_whenValid_setsDraftFalse_savesAndReturnsCard() throws Exception {
        Candidate c = new Candidate(CandidateStatus.NEW, Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(c, "id", 1L);

        when(candidateRepository.findById(1L)).thenReturn(Optional.of(c));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        String body = """
                {
                  "fullName": "John Doe",
                  "email": "john@doe.com",
                  "phone": "+12345678901",
                  "skills": "java, spring",
                  "yearsOfExperience": 5,
                  "status": "IN_REVIEW"
                }
                """;

        mockMvc.perform(put("/api/hr/candidates/1")
                        .with(httpBasic("hr", "hrPass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidate_id").value(1))
                .andExpect(jsonPath("$.draft").value(false))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.fields.full_name").value("John Doe"))
                .andExpect(jsonPath("$.fields.email").value("john@doe.com"))
                .andExpect(jsonPath("$.fields.phone").value("+12345678901"))
                .andExpect(jsonPath("$.fields.skills").value("java, spring"))
                .andExpect(jsonPath("$.fields.years_of_experience").value(5));

        // Проверяем, что draft действительно стал false и save вызвался
        verify(candidateRepository, times(1)).save(any(Candidate.class));
        assertEquals(CandidateStatus.IN_REVIEW, c.getStatus());
        assertFalse(c.isDraft());
    }

    @Test
    void updateCandidate_invalidStatus_returns400_validationErrorFromEnumParser() throws Exception {
        String body = """
                {
                  "fullName": "John Doe",
                  "email": "john@doe.com",
                  "phone": "+12345678901",
                  "skills": "java",
                  "yearsOfExperience": 5,
                  "status": "BAD_STATUS"
                }
                """;

        mockMvc.perform(put("/api/hr/candidates/1")
                        .with(httpBasic("hr", "hrPass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.status").exists())
                .andExpect(jsonPath("$.fields.status").value(org.hamcrest.Matchers.containsString("Status must be one of")));
    }
}
