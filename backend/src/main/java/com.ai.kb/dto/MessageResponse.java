package com.ai.kb.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        String id,
        String role,
        String content,
        String thinking,
        List<SourceResponse> sources,
        List<ToolCallResponse> toolCalls,
        LocalDateTime createdAt
) {
}
