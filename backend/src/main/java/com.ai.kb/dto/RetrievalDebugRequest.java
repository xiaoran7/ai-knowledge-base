package com.ai.kb.dto;

public record RetrievalDebugRequest(
        String knowledgeBaseId,
        String message,
        Integer topK
) {
}
