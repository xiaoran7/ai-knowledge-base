package com.ai.kb.repository;

import com.ai.kb.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    List<KnowledgeBase> findByUserId(String userId);

    boolean existsByName(String name);

    boolean existsByUserIdAndNameIgnoreCase(String userId, String name);
}
