package com.ai.kb.repository;

import com.ai.kb.entity.DocumentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTaskRepository extends JpaRepository<DocumentTask, String> {

    List<DocumentTask> findTop50ByUserIdAndKnowledgeBaseIdOrderByCreatedAtDesc(String userId, String knowledgeBaseId);

    List<DocumentTask> findTop50ByUserIdAndKnowledgeBaseIdAndDocumentIdOrderByCreatedAtDesc(
            String userId,
            String knowledgeBaseId,
            String documentId
    );
}
