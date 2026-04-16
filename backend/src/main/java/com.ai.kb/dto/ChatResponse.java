package com.ai.kb.dto;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String messageId,
        String title,
        String content,
        String thinking,
        List<SourceResponse> sources
) {
}
