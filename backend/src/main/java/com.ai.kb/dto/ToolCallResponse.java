package com.ai.kb.dto;

public record ToolCallResponse(
        String name,
        String title,
        String status,
        String summary,
        String detail
) {
}
