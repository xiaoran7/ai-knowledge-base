package com.ai.kb.repository;

import com.ai.kb.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    // 根据知识库 ID 查询文档（分页）
    Page<Document> findByKnowledgeBaseId(String knowledgeBaseId, Pageable pageable);

    // 根据知识库和分类查询文档（分页）
    Page<Document> findByKnowledgeBaseIdAndCategoryId(String knowledgeBaseId, String categoryId, Pageable pageable);

    // 根据知识库 ID 查询所有文档
    List<Document> findByKnowledgeBaseId(String knowledgeBaseId);

    // 删除知识库下的所有文档
    void deleteByKnowledgeBaseId(String knowledgeBaseId);

    // 统计知识库下的文档数量
    long countByKnowledgeBaseId(String knowledgeBaseId);
}