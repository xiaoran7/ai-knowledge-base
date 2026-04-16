package com.ai.kb.repository;

import com.ai.kb.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    // 根据用户和知识库查询对话列表（分页）
    Page<Conversation> findByUserIdAndKnowledgeBaseId(String userId, String knowledgeBaseId, Pageable pageable);

    // 根据用户查询所有对话
    List<Conversation> findByUserId(String userId);

    // 删除知识库下的所有对话
    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}