package com.ai.kb.controller;

import com.ai.kb.dto.CategoryRequest;
import com.ai.kb.dto.CategoryResponse;
import com.ai.kb.dto.CategoryTreeResponse;
import com.ai.kb.dto.CategoryUpdateRequest;
import com.ai.kb.dto.KnowledgeBaseRequest;
import com.ai.kb.dto.KnowledgeBaseResponse;
import com.ai.kb.service.CategoryService;
import com.ai.kb.service.KnowledgeBaseService;
import com.ai.kb.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final CategoryService categoryService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 获取知识库列表
     */
    @GetMapping
    public List<KnowledgeBaseResponse> getList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        return knowledgeBaseService.getListByUser(userId);
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public KnowledgeBaseResponse create(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody KnowledgeBaseRequest request) {
        String userId = getUserIdFromToken(authHeader);
        return knowledgeBaseService.create(userId, request);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public KnowledgeBaseResponse getById(@PathVariable String id) {
        return knowledgeBaseService.getById(id);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String userId = getUserIdFromToken(authHeader);
        knowledgeBaseService.delete(id, userId);
        return Map.of("message", "删除成功");
    }

    // ==================== 分类管理 ====================

    /**
     * 获取知识库的分类列表（扁平）
     */
    @GetMapping("/{kbId}/categories")
    public List<CategoryResponse> getCategoryList(@PathVariable String kbId) {
        return categoryService.getListByKnowledgeBase(kbId);
    }

    /**
     * 获取知识库的分类树（层级结构）
     */
    @GetMapping("/{kbId}/categories/tree")
    public List<CategoryTreeResponse> getCategoryTree(@PathVariable String kbId) {
        return categoryService.getTreeByKnowledgeBase(kbId);
    }

    /**
     * 创建分类
     */
    @PostMapping("/{kbId}/categories")
    public CategoryResponse createCategory(
            @PathVariable String kbId,
            @RequestBody CategoryRequest request) {
        return categoryService.create(kbId, request);
    }

    /**
     * 更新分类名称
     */
    @PutMapping("/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable String categoryId,
            @RequestBody CategoryUpdateRequest request) {
        return categoryService.update(categoryId, request.name());
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/categories/{categoryId}")
    public Map<String, String> deleteCategory(@PathVariable String categoryId) {
        categoryService.delete(categoryId);
        return Map.of("message", "删除成功");
    }

    /**
     * 从 Token 中提取用户 ID
     */
    private String getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未登录或 Token 无效");
        }
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}