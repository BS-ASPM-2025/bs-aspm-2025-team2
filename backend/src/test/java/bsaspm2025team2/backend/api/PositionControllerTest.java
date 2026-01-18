package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.config.SecurityConfig;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PositionController.class)
@Import(SecurityConfig.class)
class PositionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PositionRepository positionRepository;

    @Test
    void hrCannotViewPositions() throws Exception {
        mockMvc.perform(get("/api/manager/positions")
                        .with(httpBasic("hr", "hrPass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreatePosition() throws Exception {
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
            Position p = invocation.getArgument(0);
            // имитируем сохранение с id
            // (setter для id нет, поэтому просто вернём новый объект с теми же полями и id через reflection не делаем)
            // Достаточно проверить вызов save + статус 201 и поля ответа (кроме id можно не строго).
            return p;
        });

        String body = """
                {
                  "name": "Backend Intern",
                  "requiredSkills": "Java, Spring, SQL",
                  "skillsWeight": 70,
                  "experienceWeight": 30
                }
                """;

        mockMvc.perform(post("/api/manager/positions")
                        .with(httpBasic("manager", "managerPass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Backend Intern"))
                .andExpect(jsonPath("$.requiredSkills").value("Java, Spring, SQL"))
                .andExpect(jsonPath("$.skillsWeight").value(70))
                .andExpect(jsonPath("$.experienceWeight").value(30));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository, times(1)).save(captor.capture());

        Position saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Backend Intern");
        assertThat(saved.getRequiredSkills()).isEqualTo("Java, Spring, SQL");
        assertThat(saved.getSkillsWeight()).isEqualTo(70);
        assertThat(saved.getExperienceWeight()).isEqualTo(30);
    }

    @Test
    void managerCreateRejectsWeightsSumNot100() throws Exception {
        String body = """
                {
                  "name": "Backend Intern",
                  "requiredSkills": "Java",
                  "skillsWeight": 70,
                  "experienceWeight": 40
                }
                """;

        mockMvc.perform(post("/api/manager/positions")
                        .with(httpBasic("manager", "managerPass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fields.weights").value("weights must sum to 100"));

        verify(positionRepository, never()).save(any());
    }

    @Test
    void managerUpdateRejectsNegativeWeights() throws Exception {
        Position existing = new Position("Backend Intern", "Java", 70, 30);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(existing));

        String body = """
                {
                  "name": "Backend Intern",
                  "requiredSkills": "Java",
                  "skillsWeight": -1,
                  "experienceWeight": 101
                }
                """;

        mockMvc.perform(put("/api/manager/positions/1")
                        .with(httpBasic("manager", "managerPass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.skillsWeight").value("skillsWeight must be non-negative"));

        verify(positionRepository, never()).save(any());
    }
}
