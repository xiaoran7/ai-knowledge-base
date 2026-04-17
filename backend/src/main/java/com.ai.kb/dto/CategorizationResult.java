package com.ai.kb.dto;

import java.util.List;

public record CategorizationResult(
        String categoryId,
        List<String> tags
) {}

