package com.ai.kb.dto;

public record RetrievalHitResponse(
        String chunkId,
        String documentId,
        String documentTitle,
        String content,
        String chunkType,
        String chunkIndex,
        double score
) {
}
