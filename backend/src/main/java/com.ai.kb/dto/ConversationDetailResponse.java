package com.ai.kb.dto;

import java.util.List;

public record ConversationDetailResponse(
        String id,
        String title,
        String sessionSummary,
        String sessionFacts,
        List<MessageResponse> messages
) {
}
