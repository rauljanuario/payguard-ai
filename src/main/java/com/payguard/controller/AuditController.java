package com.payguard.controller;

import com.payguard.dto.AuditResponse;
import com.payguard.dto.CreateAuditRequest;
import com.payguard.dto.ReviewAuditRequest;
import com.payguard.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"})
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<AuditResponse> createAudit(
            @Valid @RequestBody CreateAuditRequest request) {
        AuditResponse response = auditService.createAudit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AuditResponse> listAudits() {
        return auditService.listAudits();
    }

    @PatchMapping("/{id}/review")
    public AuditResponse reviewAudit(
            @PathVariable Long id,
            @Valid @RequestBody ReviewAuditRequest request) {
        return auditService.reviewAudit(id, request);
    }
}
