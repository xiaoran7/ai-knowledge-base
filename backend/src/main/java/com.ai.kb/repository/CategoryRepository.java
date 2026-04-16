package com.ai.kb.repository;

import com.ai.kb.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    // 根据知识库 ID 查询分类列表
    List<Category> findByKnowledgeBaseId(String knowledgeBaseId);

    // 检查知识库下是否存在同名分类
    boolean existsByKnowledgeBaseIdAndName(String knowledgeBaseId, String name);

    // 查询某分类下的子分类
    List<Category> findByParentId(String parentId);

    // 检查是否有子分类
    boolean existsByParentId(String parentId);

    // 删除知识库下的所有分类
    void deleteByKnowledgeBaseId(String knowledgeBaseId);

    // 将文档的分类设为 null
    @Modifying
    @Query("UPDATE Document d SET d.categoryId = null WHERE d.categoryId = ?1")
    void clearDocumentCategory(String categoryId);

    // 统计分类下的文档数量
    @Query("SELECT COUNT(d) FROM Document d WHERE d.categoryId = ?1")
    long countDocumentsByCategoryId(String categoryId);
}