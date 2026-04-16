package com.ai.kb.repository;

import com.ai.kb.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // 根据对话 ID 查询消息列表（按时间升序）
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    // 删除对话下的所有消息
    void deleteByConversationId(String conversationId);

    // 统计对话的消息数量
    long countByConversationId(String conversationId);
}