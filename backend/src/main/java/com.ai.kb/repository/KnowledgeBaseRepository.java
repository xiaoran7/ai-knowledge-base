package com.ai.kb.repository;

import com.ai.kb.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    // 根据用户 ID 查询知识库列表
    List<KnowledgeBase> findByUserId(String userId);

    // 检查名称是否存在
    boolean existsByName(String name);
}