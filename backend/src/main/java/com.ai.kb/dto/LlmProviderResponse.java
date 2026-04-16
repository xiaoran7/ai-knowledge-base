package com.ai.kb.dto;

import java.util.List;

public record LlmProviderResponse(
    String provider,
    String name,
    String defaultBaseUrl,
    List<String> models,
    String defaultEmbeddingModel  // 默认 embedding 模型
) {}