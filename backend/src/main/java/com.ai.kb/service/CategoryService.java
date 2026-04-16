package com.ai.kb.service;

import com.ai.kb.dto.CategoryRequest;
import com.ai.kb.dto.CategoryResponse;
import com.ai.kb.dto.CategoryTreeResponse;
import com.ai.kb.entity.Category;
import com.ai.kb.entity.KnowledgeBase;
import com.ai.kb.repository.CategoryRepository;
import com.ai.kb.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    /**
     * 获取知识库的分类列表（扁平）
     */
    public List<CategoryResponse> getListByKnowledgeBase(String kbId) {
        return categoryRepository.findByKnowledgeBaseId(kbId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取知识库的分类树（层级结构）
     */
    public List<CategoryTreeResponse> getTreeByKnowledgeBase(String kbId) {
        List<Category> allCategories = categoryRepository.findByKnowledgeBaseId(kbId);

        // 构建父子关系映射
        Map<String, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        // 找出顶级分类（无父分类）
        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        // 构建树形结构
        return rootCategories.stream()
                .map(c -> toTreeResponse(c, childrenMap))
                .collect(Collectors.toList());
    }

    /**
     * 创建分类
     */
    @Transactional
    public CategoryResponse create(String kbId, CategoryRequest request) {
        // 检查知识库是否存在
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));

        // 检查同名分类是否存在
        if (categoryRepository.existsByKnowledgeBaseIdAndName(kbId, request.name())) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        // 如果有父分类，检查父分类是否存在且属于同一知识库
        String parentId = request.parentId();
        if (parentId != null && !parentId.isEmpty()) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父分类不存在"));
            if (!parent.getKnowledgeBaseId().equals(kbId)) {
                throw new IllegalArgumentException("父分类不属于该知识库");
            }
        } else {
            parentId = null;
        }

        Category category = new Category();
        category.setKnowledgeBaseId(kbId);
        category.setName(request.name());
        category.setParentId(parentId);
        category.setDocumentCount(0);

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    /**
     * 更新分类名称
     */
    @Transactional
    public CategoryResponse update(String categoryId, String newName) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        // 检查新名称是否已存在（同一知识库下）
        if (categoryRepository.existsByKnowledgeBaseIdAndName(category.getKnowledgeBaseId(), newName)) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        category.setName(newName);
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void delete(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));

        // 检查是否有子分类
        if (categoryRepository.existsByParentId(categoryId)) {
            throw new IllegalArgumentException("该分类下有子分类，请先删除子分类");
        }

        // 清除该分类下所有文档的分类关联
        categoryRepository.clearDocumentCategory(categoryId);

        categoryRepository.delete(category);
    }

    /**
     * 更新分类的文档数量
     */
    @Transactional
    public void updateDocumentCount(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) return;

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            long count = categoryRepository.countDocumentsByCategoryId(categoryId);
            category.setDocumentCount((int) count);
            categoryRepository.save(category);
        }
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getDocumentCount()
        );
    }

    private CategoryTreeResponse toTreeResponse(Category category, Map<String, List<Category>> childrenMap) {
        List<CategoryTreeResponse> children = new ArrayList<>();

        List<Category> childCategories = childrenMap.get(category.getId());
        if (childCategories != null) {
            children = childCategories.stream()
                    .map(c -> toTreeResponse(c, childrenMap))
                    .collect(Collectors.toList());
        }

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getDocumentCount(),
                children
        );
    }
}