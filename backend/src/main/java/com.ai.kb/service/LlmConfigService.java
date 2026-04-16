package com.ai.kb.service;

import com.ai.kb.dto.LlmConfigRequest;
import com.ai.kb.dto.LlmConfigResponse;
import com.ai.kb.dto.LlmProviderResponse;
import com.ai.kb.entity.LlmConfig;
import com.ai.kb.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LlmConfigService {

    private final LlmConfigRepository llmConfigRepository;

    // 支持的 LLM 提供商信息
    private static final Map<String, LlmProviderInfo> PROVIDERS = new LinkedHashMap<>();

    // 各提供商默认的 embedding 模型
    private static final Map<String, String> DEFAULT_EMBEDDING_MODELS = Map.of(
        "openai", "text-embedding-3-small",
        "deepseek", "text-embedding-3-small",
        "minimax", "",
        "moonshot", "text-embedding-3-small",
        "zhipu", "embedding-3",
        "aliyun", "text-embedding-v3",
        "google", "text-embedding-004",
        "anthropic", "",  // Anthropic 不支持独立 embedding
        "baidu", "",
        "custom", "text-embedding-3-small"
    );

    static {
        PROVIDERS.put("openai", new LlmProviderInfo(
                "OpenAI",
                "https://api.openai.com/v1",
                Arrays.asList("gpt-4o", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo")
        ));
        PROVIDERS.put("deepseek", new LlmProviderInfo(
                "DeepSeek",
                "https://api.deepseek.com/v1",
                Arrays.asList("deepseek-chat", "deepseek-coder")
        ));
        PROVIDERS.put("minimax", new LlmProviderInfo(
                "MiniMax",
                "https://api.minimaxi.com/v1",
                Arrays.asList(
                        "MiniMax-M2.7",
                        "MiniMax-M2.7-highspeed",
                        "MiniMax-M2.5",
                        "MiniMax-M2.5-highspeed",
                        "MiniMax-M2.1",
                        "MiniMax-M2.1-highspeed",
                        "MiniMax-M2"
                )
        ));
        PROVIDERS.put("anthropic", new LlmProviderInfo(
                "Anthropic (Claude)",
                "https://api.anthropic.com/v1",
                Arrays.asList("claude-3-opus", "claude-3-sonnet", "claude-3-haiku")
        ));
        PROVIDERS.put("google", new LlmProviderInfo(
                "Google (Gemini)",
                "https://generativelanguage.googleapis.com/v1beta",
                Arrays.asList("gemini-pro", "gemini-1.5-pro", "gemini-1.5-flash")
        ));
        PROVIDERS.put("aliyun", new LlmProviderInfo(
                "阿里云 (通义千问)",
                "https://dashscope.aliyuncs.com/api/v1",
                Arrays.asList("qwen-turbo", "qwen-plus", "qwen-max")
        ));
        PROVIDERS.put("baidu", new LlmProviderInfo(
                "百度 (文心一言)",
                "https://aip.baidubce.com/rpc/2.0/ai_custom/v1",
                Arrays.asList("ernie-4.0", "ernie-3.5", "ernie-speed")
        ));
        PROVIDERS.put("zhipu", new LlmProviderInfo(
                "智谱 AI (GLM)",
                "https://open.bigmodel.cn/api/paas/v4",
                Arrays.asList("glm-4", "glm-4-flash", "glm-3-turbo")
        ));
        PROVIDERS.put("moonshot", new LlmProviderInfo(
                "Moonshot (Kimi)",
                "https://api.moonshot.cn/v1",
                Arrays.asList("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
        ));
        PROVIDERS.put("custom", new LlmProviderInfo(
                "自定义 API",
                "",
                Collections.emptyList()
        ));
    }

    /**
     * 获取支持的 LLM 提供商列表
     */
    public List<LlmProviderResponse> getProviders() {
        return PROVIDERS.entrySet().stream()
                .map(entry -> new LlmProviderResponse(
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().defaultBaseUrl,
                        entry.getValue().models,
                        DEFAULT_EMBEDDING_MODELS.getOrDefault(entry.getKey(), "")
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的所有 LLM 配置
     */
    public List<LlmConfigResponse> getUserConfigs(String userId) {
        return llmConfigRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的默认配置
     */
    public LlmConfigResponse getDefaultConfig(String userId) {
        LlmConfig config = llmConfigRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElse(null);
        return config != null ? toResponse(config) : null;
    }

    /**
     * 创建 LLM 配置
     */
    @Transactional
    public LlmConfigResponse create(String userId, LlmConfigRequest request) {
        // 验证 provider
        if (!PROVIDERS.containsKey(request.provider()) && !"custom".equals(request.provider())) {
            throw new IllegalArgumentException("不支持的 LLM 提供商: " + request.provider());
        }

        // 检查是否已存在相同 provider 的配置
        if (llmConfigRepository.existsByUserIdAndProvider(userId, request.provider())) {
            throw new IllegalArgumentException("该提供商的配置已存在，请修改现有配置");
        }

        LlmConfig config = new LlmConfig();
        config.setUserId(userId);
        config.setProvider(request.provider());
        config.setApiKey(request.apiKey());
        config.setApiBaseUrl(normalizeApiBaseUrl(
                request.provider(),
                request.apiBaseUrl() != null ? request.apiBaseUrl() :
                        PROVIDERS.getOrDefault(request.provider(), new LlmProviderInfo("", "", Collections.emptyList())).defaultBaseUrl
        ));
        config.setModelName(request.modelName());
        config.setEmbeddingModel(request.embeddingModel() != null && !request.embeddingModel().isEmpty() ?
                request.embeddingModel() : DEFAULT_EMBEDDING_MODELS.getOrDefault(request.provider(), ""));
        config.setIsDefault(request.isDefault() != null ? request.isDefault() : false);
        config.setTemperature(request.temperature() != null ? request.temperature() : 0.7);
        config.setMaxTokens(request.maxTokens() != null ? request.maxTokens() : 4096);
        config.setIsEnabled(true);
        config.setRemark(request.remark());

        // 如果设置为默认，取消其他默认配置
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            clearDefaultConfig(userId);
        }

        LlmConfig saved = llmConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * 更新 LLM 配置
     */
    @Transactional
    public LlmConfigResponse update(String userId, String configId, LlmConfigRequest request) {
        LlmConfig config = llmConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在"));

        if (!config.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此配置");
        }

        // 更新字段
        if (request.apiKey() != null) {
            config.setApiKey(request.apiKey());
        }
        if (request.apiBaseUrl() != null) {
            config.setApiBaseUrl(normalizeApiBaseUrl(config.getProvider(), request.apiBaseUrl()));
        }
        if (request.modelName() != null) {
            config.setModelName(request.modelName());
        }
        if (request.embeddingModel() != null) {
            config.setEmbeddingModel(request.embeddingModel());
        }
        if (request.temperature() != null) {
            config.setTemperature(request.temperature());
        }
        if (request.maxTokens() != null) {
            config.setMaxTokens(request.maxTokens());
        }
        if (request.remark() != null) {
            config.setRemark(request.remark());
        }
        if (request.isDefault() != null) {
            if (Boolean.TRUE.equals(request.isDefault())) {
                clearDefaultConfig(userId);
            }
            config.setIsDefault(request.isDefault());
        }

        LlmConfig saved = llmConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * 设置为默认配置
     */
    @Transactional
    public void setDefault(String userId, String configId) {
        LlmConfig config = llmConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在"));

        if (!config.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此配置");
        }

        clearDefaultConfig(userId);
        config.setIsDefault(true);
        llmConfigRepository.save(config);
    }

    /**
     * 启用/禁用配置
     */
    @Transactional
    public void setEnabled(String userId, String configId, boolean enabled) {
        LlmConfig config = llmConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在"));

        if (!config.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此配置");
        }

        config.setIsEnabled(enabled);
        llmConfigRepository.save(config);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void delete(String userId, String configId) {
        LlmConfig config = llmConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在"));

        if (!config.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此配置");
        }

        llmConfigRepository.delete(config);
    }

    private void clearDefaultConfig(String userId) {
        llmConfigRepository.findByUserId(userId)
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                .forEach(c -> {
                    c.setIsDefault(false);
                    llmConfigRepository.save(c);
                });
    }

    private String normalizeApiBaseUrl(String provider, String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return apiBaseUrl;
        }

        String normalized = apiBaseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (isMiniMaxConfig(provider, normalized)) {
            return normalizeMiniMaxBaseUrl(normalized);
        }

        return normalized;
    }

    private String normalizeMiniMaxBaseUrl(String baseUrl) {
        String normalized = baseUrl;
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean anthropicPath = lower.contains("/anthropic");
        boolean hasV1Suffix = lower.endsWith("/v1");

        if (lower.contains("api.minimax.io")) {
            normalized = "https://api.minimax.io";
        } else if (lower.contains("api.minimaxi.com")) {
            normalized = "https://api.minimaxi.com";
        }

        if (anthropicPath || hasV1Suffix) {
            return normalized + "/v1";
        }

        return normalized + "/v1";
    }

    private boolean isMiniMaxConfig(String provider, String apiBaseUrl) {
        if ("minimax".equals(provider)) {
            return true;
        }
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return false;
        }
        String lower = apiBaseUrl.toLowerCase(Locale.ROOT);
        return lower.contains("minimax.io") || lower.contains("minimaxi.com");
    }

    private LlmConfigResponse toResponse(LlmConfig config) {
        LlmProviderInfo info = PROVIDERS.getOrDefault(config.getProvider(), new LlmProviderInfo("未知", "", Collections.emptyList()));
        String apiBaseUrl = isMiniMaxConfig(config.getProvider(), config.getApiBaseUrl())
                ? normalizeApiBaseUrl(config.getProvider(), config.getApiBaseUrl())
                : config.getApiBaseUrl();
        return new LlmConfigResponse(
                config.getId(),
                config.getProvider(),
                info.name,
                maskApiKey(config.getApiKey()),
                apiBaseUrl,
                config.getModelName(),
                config.getEmbeddingModel(),
                config.getIsDefault(),
                config.getIsEnabled(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getRemark(),
                config.getCreatedAt() != null ? config.getCreatedAt().toString() : null
        );
    }

    // 隐藏 API Key 的中间部分
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return apiKey;
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    // 提供商信息内部类
    private static class LlmProviderInfo {
        String name;
        String defaultBaseUrl;
        List<String> models;

        LlmProviderInfo(String name, String defaultBaseUrl, List<String> models) {
            this.name = name;
            this.defaultBaseUrl = defaultBaseUrl;
            this.models = models;
        }
    }
}
