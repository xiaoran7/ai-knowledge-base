package com.ai.kb.dto;

public record CategoryRequest(
    String name,
    String parentId
) {}