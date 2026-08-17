package dev.esgenius.controller;

import dev.esgenius.dto.FrameworkRequirementResponse;
import dev.esgenius.dto.FrameworkResponse;
import dev.esgenius.entity.EsgCategory;
import dev.esgenius.service.FrameworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/frameworks")
public class FrameworkController {

    private final FrameworkService frameworkService;

    public FrameworkController(FrameworkService frameworkService) {
        this.frameworkService = frameworkService;
    }

    @GetMapping
    public ResponseEntity<List<FrameworkResponse>> listFrameworks() {
        return ResponseEntity.ok(frameworkService.listFrameworks());
    }

    @GetMapping("/{frameworkId}")
    public ResponseEntity<FrameworkResponse> getFramework(@PathVariable Long frameworkId) {
        return ResponseEntity.ok(frameworkService.getFramework(frameworkId));
    }

    @GetMapping("/{frameworkId}/requirements")
    public ResponseEntity<List<FrameworkRequirementResponse>> getRequirements(
            @PathVariable Long frameworkId,
            @RequestParam(required = false) EsgCategory category) {
        return ResponseEntity.ok(frameworkService.getRequirements(frameworkId, category));
    }
}
