package com.payguard.dto;

import java.util.List;

public record GeminiGenerationRequest(
        List<GeminiContent> contents,
        GeminiGenerationConfig generationConfig
) {
    public record GeminiContent(
            List<GeminiPart> parts
    ) {
    }

    public record GeminiPart(
            String text
    ) {
    }

    public record GeminiGenerationConfig(
            String responseMimeType
    ) {
    }
}
