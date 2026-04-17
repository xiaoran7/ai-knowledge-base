package com.ai.kb.dto;

public record KnowledgeGraphStatsResponse(
        int totalNodes,
        int totalEdges,
        int documentNodes,
        int categoryNodes,
        int referenceEdges,
        int sharedTagEdges,
        int membershipEdges,
        int orphanDocuments
) {
}
