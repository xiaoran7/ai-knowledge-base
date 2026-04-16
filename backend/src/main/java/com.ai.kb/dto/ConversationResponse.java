package com.ai.kb.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
    String id,
    String title,
    int messageCount,
    LocalDateTime lastMessageAt
) {}