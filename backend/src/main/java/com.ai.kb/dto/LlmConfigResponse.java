package com.ai.kb.dto;

public record LlmConfigResponse(
    String id,
    String provider,
    String providerName,
    String apiKey,
    String apiBaseUrl,
    String modelName,
    String embeddingModel,  // embedding 模型名称
    Boolean isDefault,
    Boolean isEnabled,
    Double temperature,
    Integer maxTokens,
    String remark,
    String createdAt
) {}