package com.ai.kb.service;

import com.ai.kb.dto.KnowledgeBaseRequest;
import com.ai.kb.dto.KnowledgeBaseResponse;
import com.ai.kb.entity.KnowledgeBase;
import com.ai.kb.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    /**
     * 获取用户的知识库列表
     */
    public List<KnowledgeBaseResponse> getListByUser(String userId) {
        return knowledgeBaseRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBaseResponse create(String userId, KnowledgeBaseRequest request) {
        if (knowledgeBaseRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("知识库名称已存在");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setUserId(userId);
        kb.setDocumentCount(0);

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return toResponse(saved);
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void delete(String id, String userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));

        if (!kb.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此知识库");
        }

        knowledgeBaseRepository.delete(kb);
    }

    /**
     * 获取知识库详情
     */
    public KnowledgeBaseResponse getById(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));
        return toResponse(kb);
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        return new KnowledgeBaseResponse(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                kb.getDocumentCount(),
                kb.getCreatedAt()
        );
    }
}