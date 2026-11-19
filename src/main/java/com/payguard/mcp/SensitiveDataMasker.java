package com.payguard.mcp;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveDataMasker {

    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)(bearer\\s+[A-Za-z0-9._~+\\-/]+=*)");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(\\b(?:password|passwd|pwd|token|secret|api[_-]?key|access[_-]?key|client[_-]?secret)\\b\\s*[=:]\\s*)"
                    + "(\"[^\"]*\"|'[^']*'|[^\\s,;}]+)");
    private static final Pattern CPF = Pattern.compile(
            "\\b(?:\\d{3}[.]?\\d{3}[.]?\\d{3}-?\\d{2})\\b");
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
            "(?i)(authorization\\s*[=:]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,;}]+)");

    public String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String maskedText = text;
        maskedText = BEARER_TOKEN.matcher(maskedText).replaceAll("Bearer [REDACTED]");
        maskedText = KEY_VALUE_SECRET.matcher(maskedText)
                .replaceAll("$1[REDACTED]");
        maskedText = AUTHORIZATION_HEADER.matcher(maskedText)
                .replaceAll("$1[REDACTED]");
        return CPF.matcher(maskedText).replaceAll("[CPF_REDACTED]");
    }
}
