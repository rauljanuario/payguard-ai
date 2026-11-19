package com.payguard.dto;

import com.payguard.model.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewAuditRequest(
        @NotNull ReviewStatus reviewStatus
) {
}
