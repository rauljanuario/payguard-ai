package com.payguard.service;

import com.payguard.client.GeminiClient;
import com.payguard.dto.AuditResponse;
import com.payguard.dto.CreateAuditRequest;
import com.payguard.dto.GeminiAnalysisResponse;
import com.payguard.dto.ReviewAuditRequest;
import com.payguard.exception.AuditNotFoundException;
import com.payguard.mcp.SensitiveDataMasker;
import com.payguard.model.AuditReport;
import com.payguard.model.AuditSeverity;
import com.payguard.model.ReviewStatus;
import com.payguard.repository.AuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private SensitiveDataMasker sensitiveDataMasker;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(
                auditRepository,
                geminiClient,
                sensitiveDataMasker);
    }

    @Test
    void shouldMaskInputAnalyzeAndSaveAudit() {
        CreateAuditRequest request = new CreateAuditRequest(
                "password=secret",
                "POST /payments",
                "payments");
        GeminiAnalysisResponse analysis = new GeminiAnalysisResponse(
                true,
                AuditSeverity.HIGH,
                List.of("The response field changed"),
                "# Payments API");

        when(sensitiveDataMasker.mask("password=secret")).thenReturn("password=[REDACTED]");
        when(sensitiveDataMasker.mask("POST /payments")).thenReturn("POST /payments");
        when(geminiClient.analyze(
                "payments",
                "password=[REDACTED]",
                "POST /payments"))
                .thenReturn(analysis);
        when(auditRepository.save(any(AuditReport.class)))
                .thenAnswer(invocation -> {
                    AuditReport report = invocation.getArgument(0);
                    setReportId(report, 10L);
                    setReportCreatedAt(report, Instant.parse("2026-11-18T18:00:00Z"));
                    return report;
                });

        AuditResponse response = auditService.createAudit(request);

        assertEquals(10L, response.id());
        assertEquals("payments", response.moduleName());
        assertTrue(response.hasBreakingChange());
        assertEquals(AuditSeverity.HIGH, response.severity());

        ArgumentCaptor<AuditReport> captor = ArgumentCaptor.forClass(AuditReport.class);
        verify(auditRepository).save(captor.capture());
        assertEquals("password=[REDACTED]", captor.getValue().getSourceCode());
        assertEquals("POST /payments", captor.getValue().getCurrentDocumentation());
    }

    @Test
    void shouldListAuditsOrderedByNewestFirst() {
        AuditReport report = new AuditReport(
                "webhooks",
                "masked source",
                "masked documentation",
                false,
                AuditSeverity.LOW,
                List.of(),
                "# Webhooks");
        setReportId(report, 7L);
        setReportCreatedAt(report, Instant.parse("2026-11-18T18:00:00Z"));
        when(auditRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(report));

        List<AuditResponse> responses = auditService.listAudits();

        assertEquals(1, responses.size());
        assertEquals(7L, responses.get(0).id());
        assertEquals("webhooks", responses.get(0).moduleName());
        verify(auditRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldApproveAudit() {
        AuditReport report = new AuditReport(
                "checkout",
                "source",
                "documentation",
                true,
                AuditSeverity.MEDIUM,
                List.of("Changed required field"),
                "# Checkout");
        setReportId(report, 15L);
        setReportCreatedAt(report, Instant.parse("2026-11-18T18:00:00Z"));
        when(auditRepository.findById(15L)).thenReturn(Optional.of(report));

        AuditResponse response = auditService.reviewAudit(
                15L,
                new ReviewAuditRequest(ReviewStatus.APPROVED));

        assertEquals(ReviewStatus.APPROVED, response.reviewStatus());
        assertEquals(ReviewStatus.APPROVED, report.getReviewStatus());
    }

    @Test
    void shouldRejectAudit() {
        AuditReport report = new AuditReport(
                "checkout",
                "source",
                "documentation",
                true,
                AuditSeverity.MEDIUM,
                List.of("Changed required field"),
                "# Checkout");
        setReportId(report, 16L);
        setReportCreatedAt(report, Instant.parse("2026-11-18T18:00:00Z"));
        when(auditRepository.findById(16L)).thenReturn(Optional.of(report));

        AuditResponse response = auditService.reviewAudit(
                16L,
                new ReviewAuditRequest(ReviewStatus.REJECTED));

        assertEquals(ReviewStatus.REJECTED, response.reviewStatus());
        assertEquals(ReviewStatus.REJECTED, report.getReviewStatus());
    }

    @Test
    void shouldThrowWhenAuditDoesNotExist() {
        when(auditRepository.findById(99L)).thenReturn(Optional.empty());

        AuditNotFoundException exception = assertThrows(
                AuditNotFoundException.class,
                () -> auditService.reviewAudit(
                        99L,
                        new ReviewAuditRequest(ReviewStatus.REJECTED)));

        assertTrue(exception.getMessage().contains("99"));
    }

    private void setReportId(AuditReport report, Long id) {
        try {
            java.lang.reflect.Field field = AuditReport.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(report, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set test audit ID", exception);
        }
    }

    private void setReportCreatedAt(AuditReport report, Instant createdAt) {
        try {
            java.lang.reflect.Field field =
                    AuditReport.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(report, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not set test audit timestamp",
                    exception);
        }
    }
}
