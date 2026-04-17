package com.ai.kb.dto;

import java.util.List;

public record KnowledgeGraphNodeResponse(
        String id,
        String type,
        String title,
        String categoryId,
        String categoryName,
        String status,
        String summaryType,
        List<String> tags,
        Integer degree,
        Integer inbound,
        Integer outbound,
        boolean virtualNode
) {
}
