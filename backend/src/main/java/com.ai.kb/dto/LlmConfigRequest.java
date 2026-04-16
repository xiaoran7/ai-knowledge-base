package com.ai.kb.dto;

public record LlmConfigRequest(
    String provider,
    String apiKey,
    String apiBaseUrl,
    String modelName,
    String embeddingModel,  // embedding 模型名称
    Boolean isDefault,
    Double temperature,
    Integer maxTokens,
    String remark
) {}