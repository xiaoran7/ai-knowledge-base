package com.ai.kb.dto;

public record ChatRequest(
    String knowledgeBaseId,
    String message,
    String conversationId
) {}