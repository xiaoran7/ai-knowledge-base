package com.ai.kb.dto;

public record ConversationMemoryUpdateRequest(
        String sessionSummary,
        String sessionFacts
) {
}
