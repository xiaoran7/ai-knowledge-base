package com.ai.kb.dto;

public record ConversationExportResponse(
        String fileName,
        byte[] content,
        String contentType
) {
}
