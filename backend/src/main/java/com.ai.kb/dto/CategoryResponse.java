package com.ai.kb.dto;

public record CategoryResponse(
    String id,
    String name,
    String parentId,
    Integer documentCount
) {}