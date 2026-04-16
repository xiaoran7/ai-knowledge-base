package com.ai.kb.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "llm_configs")
public class LlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "provider", nullable = false)
    private String provider;  // openai, deepseek, anthropic, google, aliyun, baidu

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "api_base_url")
    private String apiBaseUrl;  // 自定义 API 地址

    @Column(name = "model_name")
    private String modelName;  // 具体模型名称（对话模型）

    @Column(name = "embedding_model")
    private String embeddingModel;  // embedding 模型名称

    @Column(name = "is_default")
    private Boolean isDefault = false;  // 是否为默认配置

    @Column(name = "temperature")
    private Double temperature = 0.7;

    @Column(name = "max_tokens")
    private Integer maxTokens = 4096;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "remark")
    private String remark;  // 备注

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}