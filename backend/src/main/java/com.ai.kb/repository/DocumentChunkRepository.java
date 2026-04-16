package com.ai.kb.repository;

import com.ai.kb.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    // 根据文档 ID 查询切片
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);

    // 根据知识库 ID 查询所有切片
    List<DocumentChunk> findByKnowledgeBaseId(String knowledgeBaseId);

    // 删除文档的所有切片
    void deleteByDocumentId(String documentId);

    // 统计文档的切片数量
    long countByDocumentId(String documentId);

    // 批量删除知识库的所有切片
    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.knowledgeBaseId = ?1")
    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}