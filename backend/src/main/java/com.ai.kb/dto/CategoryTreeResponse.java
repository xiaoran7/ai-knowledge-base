package com.ai.kb.dto;

import java.util.List;

public record CategoryTreeResponse(
    String id,
    String name,
    Integer documentCount,
    List<CategoryTreeResponse> children
) {}