package com.payguard.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_reports")
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String currentDocumentation;

    @Column(nullable = false)
    private boolean hasBreakingChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditSeverity severity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "audit_discrepancies", joinColumns = @JoinColumn(name = "audit_report_id"))
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> discrepancies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestedDocMarkdown;

    @Enumerated(EnumType.STRING)
    private ReviewStatus reviewStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditReport() {
    }

    public AuditReport(
            String moduleName,
            String sourceCode,
            String currentDocumentation,
            boolean hasBreakingChange,
            AuditSeverity severity,
            List<String> discrepancies,
            String suggestedDocMarkdown) {
        this.moduleName = moduleName;
        this.sourceCode = sourceCode;
        this.currentDocumentation = currentDocumentation;
        this.hasBreakingChange = hasBreakingChange;
        this.severity = severity;
        this.discrepancies = new ArrayList<>(discrepancies);
        this.suggestedDocMarkdown = suggestedDocMarkdown;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getCurrentDocumentation() {
        return currentDocumentation;
    }

    public boolean hasBreakingChange() {
        return hasBreakingChange;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    public List<String> getDiscrepancies() {
        return List.copyOf(discrepancies);
    }

    public String getSuggestedDocMarkdown() {
        return suggestedDocMarkdown;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void review(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
}
