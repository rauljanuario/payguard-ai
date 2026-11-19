package com.payguard.dto;

import java.util.List;

public record GeminiGenerationResponse(
        List<Candidate> candidates
) {
    public record Candidate(
            GeminiContent content
    ) {
    }

    public record GeminiContent(
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }
}
