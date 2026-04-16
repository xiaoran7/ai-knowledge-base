package com.ai.kb.controller;

import com.ai.kb.dto.LlmConfigRequest;
import com.ai.kb.dto.LlmConfigResponse;
import com.ai.kb.dto.LlmProviderResponse;
import com.ai.kb.service.LlmConfigService;
import com.ai.kb.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/llm-config")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmConfigService llmConfigService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 获取支持的 LLM 提供商列表
     */
    @GetMapping("/providers")
    public List<LlmProviderResponse> getProviders() {
        return llmConfigService.getProviders();
    }

    /**
     * 获取用户的所有 LLM 配置
     */
    @GetMapping
    public List<LlmConfigResponse> getUserConfigs(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return llmConfigService.getUserConfigs(userId);
    }

    /**
     * 获取用户的默认配置
     */
    @GetMapping("/default")
    public LlmConfigResponse getDefaultConfig(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return llmConfigService.getDefaultConfig(userId);
    }

    /**
     * 创建 LLM 配置
     */
    @PostMapping
    public LlmConfigResponse create(
            @RequestBody LlmConfigRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return llmConfigService.create(userId, request);
    }

    /**
     * 更新 LLM 配置
     */
    @PutMapping("/{id}")
    public LlmConfigResponse update(
            @PathVariable String id,
            @RequestBody LlmConfigRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return llmConfigService.update(userId, id, request);
    }

    /**
     * 设置为默认配置
     */
    @PutMapping("/{id}/default")
    public Map<String, String> setDefault(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        llmConfigService.setDefault(userId, id);
        return Map.of("message", "设置成功");
    }

    /**
     * 启用/禁用配置
     */
    @PutMapping("/{id}/enabled")
    public Map<String, String> setEnabled(
            @PathVariable String id,
            @RequestParam boolean enabled,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        llmConfigService.setEnabled(userId, id, enabled);
        return Map.of("message", enabled ? "已启用" : "已禁用");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        llmConfigService.delete(userId, id);
        return Map.of("message", "删除成功");
    }

    private String getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录或 Token 无效");
        }
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}