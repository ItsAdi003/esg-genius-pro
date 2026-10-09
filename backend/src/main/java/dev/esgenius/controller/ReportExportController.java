package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.dto.ReportExportResponse;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.ReportExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportExportController {

    private final ReportExportService reportExportService;
    private final DocumentAccessPolicy documentAccessPolicy;

    public ReportExportController(
            ReportExportService reportExportService,
            DocumentAccessPolicy documentAccessPolicy) {
        this.reportExportService = reportExportService;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @GetMapping
    public ResponseEntity<List<ReportExportResponse>> listReports(HttpServletRequest request) {
        return ResponseEntity.ok(reportExportService.listRecent(
                documentAccessPolicy.resolve(AuthenticatedUser.from(request))));
    }
}
