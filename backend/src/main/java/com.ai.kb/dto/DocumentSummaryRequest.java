package com.ai.kb.dto;

public record DocumentSummaryRequest(
        String content,
        String summaryMode
) {
}
