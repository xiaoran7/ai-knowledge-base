package com.ai.kb.dto;

public record DocumentUploadResponse(
        String documentId,
        String title,
        String status,
        String summaryContent,
        String summaryType
) {
}
