package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.PositionResponse;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class PositionsController {

    private final PositionRepository positionRepository;

    public PositionsController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @GetMapping
    public List<PositionResponse> list() {
        return positionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PositionResponse get(@PathVariable Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));
        return toResponse(position);
    }

    private PositionResponse toResponse(Position p) {
        return new PositionResponse(
                p.getId(),
                p.getName(),
                p.getRequiredSkills(),
                p.getSkillsWeight(),
                p.getExperienceWeight()
        );
    }
}
