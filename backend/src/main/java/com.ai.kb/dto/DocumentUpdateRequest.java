package com.ai.kb.dto;

public record DocumentUpdateRequest(
        String content,
        String summaryContent,
        String summaryMode,
        Boolean regenerateSummary
) {
}
