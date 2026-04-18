package com.ai.kb.repository;

import com.ai.kb.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Page<Document> findByKnowledgeBaseId(String knowledgeBaseId, Pageable pageable);

    Page<Document> findByKnowledgeBaseIdAndCategoryId(String knowledgeBaseId, String categoryId, Pageable pageable);

    List<Document> findByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);

    long countByKnowledgeBaseId(String knowledgeBaseId);

    boolean existsByKnowledgeBaseIdAndTitleIgnoreCase(String knowledgeBaseId, String title);
}
