package com.ai.kb.dto;

import java.util.List;

public record DocumentUpdateRequest(
        String content,
        String summaryContent,
        String summaryMode,
        Boolean regenerateSummary,
        List<String> tags
) {
}
