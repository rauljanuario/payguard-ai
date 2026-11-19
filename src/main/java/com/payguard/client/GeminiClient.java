package com.payguard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payguard.dto.GeminiAnalysisResponse;
import com.payguard.dto.GeminiGenerationRequest;
import com.payguard.dto.GeminiGenerationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api.url}") String apiUrl,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("Gemini API URL is not configured");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("Gemini API model is not configured");
        }
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured. Set it as an environment variable or in .env.");
        }
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeminiAnalysisResponse analyze(
            String moduleName,
            String maskedSourceCode,
            String maskedDocumentation) {
        GeminiGenerationRequest request = createRequest(
                moduleName,
                maskedSourceCode,
                maskedDocumentation);

        GeminiGenerationResponse response = restClient.post()
                .uri(apiUrl + "/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .body(request)
                .retrieve()
                .body(GeminiGenerationResponse.class);

        String responseText = extractResponseText(response);
        return parseAnalysisResponse(responseText);
    }

    private GeminiGenerationRequest createRequest(
            String moduleName,
            String sourceCode,
            String documentation) {
        String prompt = """
                Compare the source code with the current API/webhook documentation.
                Detect breaking changes and suggest corrected documentation.

                Return ONLY a valid JSON object with this exact structure:
                {
                  "hasBreakingChange": boolean,
                  "severity": "LOW" | "MEDIUM" | "HIGH",
                  "discrepancies": ["clear discrepancy"],
                  "suggestedDocMarkdown": "full corrected Markdown documentation"
                }

                Consider route changes, HTTP method changes, request/response field removals or type changes,
                required fields, status codes, authentication, and webhook payload changes.
                Do not include Markdown fences or explanations outside the JSON object.

                Module name:
                %s

                Source code:
                %s

                Current documentation:
                %s
                """.formatted(moduleName, sourceCode, documentation);

        GeminiGenerationRequest.GeminiPart part =
                new GeminiGenerationRequest.GeminiPart(prompt);
        GeminiGenerationRequest.GeminiContent content =
                new GeminiGenerationRequest.GeminiContent(List.of(part));
        GeminiGenerationRequest.GeminiGenerationConfig generationConfig =
                new GeminiGenerationRequest.GeminiGenerationConfig("application/json");

        return new GeminiGenerationRequest(List.of(content), generationConfig);
    }

    private String extractResponseText(GeminiGenerationResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()
                || response.candidates().get(0).content() == null
                || response.candidates().get(0).content().parts() == null
                || response.candidates().get(0).content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini returned an empty response");
        }

        String text = response.candidates().get(0).content().parts().stream()
                .map(GeminiGenerationResponse.Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty response");
        }
        return text;
    }

    private GeminiAnalysisResponse parseAnalysisResponse(String responseText) {
        String normalizedText = responseText.trim();
        if (normalizedText.startsWith("```")) {
            normalizedText = normalizedText
                    .replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }

        try {
            return objectMapper.readValue(normalizedText, GeminiAnalysisResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini returned invalid JSON", exception);
        }
    }
}
