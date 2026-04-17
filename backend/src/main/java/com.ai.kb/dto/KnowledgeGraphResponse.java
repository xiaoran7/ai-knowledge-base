package com.ai.kb.dto;

import java.util.List;

public record KnowledgeGraphResponse(
        String knowledgeBaseId,
        String knowledgeBaseName,
        List<KnowledgeGraphNodeResponse> nodes,
        List<KnowledgeGraphEdgeResponse> edges,
        KnowledgeGraphStatsResponse stats
) {
}
