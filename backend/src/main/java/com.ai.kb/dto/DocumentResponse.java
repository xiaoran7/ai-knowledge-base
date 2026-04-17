package com.ai.kb.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        String id,
        String title,
        String fileType,
        Long fileSize,
        String status,
        String processingStage,
        String categoryId,
        String categoryName,
        List<String> tags,
        LocalDateTime createdAt,
        String content,
        String summaryContent,
        String summaryType,
        LocalDateTime summaryUpdatedAt,
        String lastError
) {
}
