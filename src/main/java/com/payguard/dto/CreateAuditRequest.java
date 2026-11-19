package com.payguard.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAuditRequest(
        @NotBlank String sourceCode,
        @NotBlank String currentDocumentation,
        @NotBlank String moduleName
) {
}
