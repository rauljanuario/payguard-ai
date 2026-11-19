package com.payguard.dto;

import com.payguard.model.AuditSeverity;

import java.util.List;

public record GeminiAnalysisResponse(
        boolean hasBreakingChange,
        AuditSeverity severity,
        List<String> discrepancies,
        String suggestedDocMarkdown
) {
}
