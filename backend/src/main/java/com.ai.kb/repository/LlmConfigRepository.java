package com.ai.kb.repository;

import com.ai.kb.entity.LlmConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LlmConfigRepository extends JpaRepository<LlmConfig, String> {

    // 根据用户 ID 查询所有配置
    List<LlmConfig> findByUserId(String userId);

    // 根据用户 ID 查询启用的配置
    List<LlmConfig> findByUserIdAndIsEnabledTrue(String userId);

    // 根据用户 ID 查询默认配置
    Optional<LlmConfig> findByUserIdAndIsDefaultTrue(String userId);

    // 检查用户是否已有某个 provider 的配置
    boolean existsByUserIdAndProvider(String userId, String provider);

    // 根据用户 ID 和 provider 查询配置
    Optional<LlmConfig> findByUserIdAndProvider(String userId, String provider);
}