package com.ai.kb.dto;

public record SourceResponse(
    String documentId,
    String documentTitle,
    String content,
    double score
) {}