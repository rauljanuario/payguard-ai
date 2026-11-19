package com.payguard.dto;

import com.payguard.model.AuditSeverity;
import com.payguard.model.ReviewStatus;

import java.time.Instant;
import java.util.List;

public record AuditResponse(
        Long id,
        String moduleName,
        boolean hasBreakingChange,
        AuditSeverity severity,
        List<String> discrepancies,
        String suggestedDocMarkdown,
        ReviewStatus reviewStatus,
        Instant createdAt
) {
}
