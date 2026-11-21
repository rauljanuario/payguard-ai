package com.payguard.service;

import com.payguard.client.GeminiClient;
import com.payguard.dto.AuditResponse;
import com.payguard.dto.CreateAuditRequest;
import com.payguard.dto.GeminiAnalysisResponse;
import com.payguard.dto.ReviewAuditRequest;
import com.payguard.exception.AuditNotFoundException;
import com.payguard.security.SensitiveDataMasker;
import com.payguard.model.AuditReport;
import com.payguard.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final GeminiClient geminiClient;
    private final SensitiveDataMasker sensitiveDataMasker;

    public AuditService(
            AuditRepository auditRepository,
            GeminiClient geminiClient,
            SensitiveDataMasker sensitiveDataMasker) {
        this.auditRepository = auditRepository;
        this.geminiClient = geminiClient;
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    @Transactional
    public AuditResponse createAudit(CreateAuditRequest request) {
        String maskedSourceCode = sensitiveDataMasker.mask(request.sourceCode());
        String maskedDocumentation =
                sensitiveDataMasker.mask(request.currentDocumentation());

        GeminiAnalysisResponse analysis = geminiClient.analyze(
                request.moduleName(),
                maskedSourceCode,
                maskedDocumentation);

        AuditReport report = new AuditReport(
                request.moduleName(),
                maskedSourceCode,
                maskedDocumentation,
                analysis.hasBreakingChange(),
                analysis.severity(),
                analysis.discrepancies(),
                analysis.suggestedDocMarkdown());

        return toResponse(auditRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> listAudits() {
        return auditRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AuditResponse reviewAudit(Long auditId, ReviewAuditRequest request) {
        AuditReport report = auditRepository.findById(auditId)
                .orElseThrow(() -> new AuditNotFoundException(auditId));

        report.review(request.reviewStatus());
        return toResponse(report);
    }

    private AuditResponse toResponse(AuditReport report) {
        return new AuditResponse(
                report.getId(),
                report.getModuleName(),
                report.hasBreakingChange(),
                report.getSeverity(),
                report.getDiscrepancies(),
                report.getSuggestedDocMarkdown(),
                report.getReviewStatus(),
                report.getCreatedAt());
    }
}
