package com.ai.kb.dto;

import java.time.LocalDateTime;

public record DocumentTaskResponse(
        String id,
        String documentId,
        String documentTitle,
        String taskType,
        String status,
        String processingStage,
        String summaryMode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
