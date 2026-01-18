package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.PositionRequest;
import bsaspm2025team2.backend.api.dto.PositionResponse;
import bsaspm2025team2.backend.domain.Position;
import bsaspm2025team2.backend.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/manager/positions")
public class PositionController {

    private final PositionRepository positionRepository;

    public PositionController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse create(@RequestBody PositionRequest request) {
        validateWeights(request);
        Position position = new Position(
                request.name(),
                request.requiredSkills(),
                request.skillsWeight(),
                request.experienceWeight()
        );

        Position saved = positionRepository.save(position);
        return toResponse(saved);
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

    @PutMapping("/{id}")
    public PositionResponse update(@PathVariable Long id,
                                   @RequestBody PositionRequest request) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));

        validateWeights(request);
        position.setName(request.name());
        position.setRequiredSkills(request.requiredSkills());
        position.setSkillsWeight(request.skillsWeight());
        position.setExperienceWeight(request.experienceWeight());

        Position saved = positionRepository.save(position);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!positionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found");
        }
        positionRepository.deleteById(id);
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
    private void validateWeights(PositionRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.skillsWeight() < 0) {
            errors.put("skillsWeight", "skillsWeight must be non-negative");
        }
        if (request.experienceWeight() < 0) {
            errors.put("experienceWeight", "experienceWeight must be non-negative");
        }

        int sum = request.skillsWeight() + request.experienceWeight();
        if (sum != 100) {
            errors.put("weights", "weights must sum to 100");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
