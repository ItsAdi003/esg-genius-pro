package dev.esgenius.service;

import dev.esgenius.dto.FrameworkRequirementResponse;
import dev.esgenius.dto.FrameworkResponse;
import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FrameworkService {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkRequirementRepository requirementRepository;

    public FrameworkService(FrameworkRepository frameworkRepository,
                            FrameworkRequirementRepository requirementRepository) {
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
    }

    public List<FrameworkResponse> listFrameworks() {
        return frameworkRepository.findAll().stream()
                .map(f -> {
                    int count = requirementRepository.findByFramework(f).size();
                    return FrameworkResponse.from(f, count);
                })
                .toList();
    }

    public FrameworkResponse getFramework(Long id) {
        Framework framework = frameworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Framework not found: " + id));
        int count = requirementRepository.findByFramework(framework).size();
        return FrameworkResponse.from(framework, count);
    }

    public List<FrameworkRequirementResponse> getRequirements(Long frameworkId, EsgCategory category) {
        Framework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Framework not found: " + frameworkId));

        List<FrameworkRequirement> requirements;
        if (category != null) {
            requirements = requirementRepository.findByFrameworkAndCategory(framework, category);
        } else {
            requirements = requirementRepository.findByFramework(framework);
        }

        return requirements.stream()
                .map(FrameworkRequirementResponse::from)
                .toList();
    }
}
