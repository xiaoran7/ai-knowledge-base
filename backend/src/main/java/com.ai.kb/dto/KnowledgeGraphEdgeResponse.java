package com.ai.kb.dto;

public record KnowledgeGraphEdgeResponse(
        String id,
        String source,
        String target,
        String type,
        String label,
        Double weight
) {
}
