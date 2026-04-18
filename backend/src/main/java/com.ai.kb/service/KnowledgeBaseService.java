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

    public List<KnowledgeBaseResponse> getListByUser(String userId) {
        return knowledgeBaseRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public KnowledgeBaseResponse create(String userId, KnowledgeBaseRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (knowledgeBaseRepository.existsByUserIdAndNameIgnoreCase(userId, request.getName().trim())) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u540d\u79f0\u5df2\u5b58\u5728");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName().trim());
        kb.setDescription(request.getDescription());
        kb.setUserId(userId);
        kb.setDocumentCount(0);

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return toResponse(saved);
    }

    @Transactional
    public KnowledgeBaseResponse rename(String id, String userId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }

        KnowledgeBase kb = getOwnedKnowledgeBase(id, userId);
        String normalizedName = newName.trim();
        if (!kb.getName().equalsIgnoreCase(normalizedName)
                && knowledgeBaseRepository.existsByUserIdAndNameIgnoreCase(userId, normalizedName)) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u540d\u79f0\u5df2\u5b58\u5728");
        }

        kb.setName(normalizedName);
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return toResponse(saved);
    }

    @Transactional
    public KnowledgeBaseResponse update(String id, String userId, KnowledgeBaseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u8bf7\u6c42\u4e0d\u80fd\u4e3a\u7a7a");
        }

        KnowledgeBase kb = getOwnedKnowledgeBase(id, userId);
        String normalizedName = request.getName() == null ? "" : request.getName().trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!kb.getName().equalsIgnoreCase(normalizedName)
                && knowledgeBaseRepository.existsByUserIdAndNameIgnoreCase(userId, normalizedName)) {
            throw new IllegalArgumentException("\u77e5\u8bc6\u5e93\u540d\u79f0\u5df2\u5b58\u5728");
        }

        kb.setName(normalizedName);
        kb.setDescription(request.getDescription());

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id, String userId) {
        KnowledgeBase kb = getOwnedKnowledgeBase(id, userId);
        knowledgeBaseRepository.delete(kb);
    }

    public KnowledgeBaseResponse getById(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("\u77e5\u8bc6\u5e93\u4e0d\u5b58\u5728"));
        return toResponse(kb);
    }

    private KnowledgeBase getOwnedKnowledgeBase(String id, String userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("\u77e5\u8bc6\u5e93\u4e0d\u5b58\u5728"));

        if (!kb.getUserId().equals(userId)) {
            throw new IllegalArgumentException("\u65e0\u6743\u64cd\u4f5c\u6b64\u77e5\u8bc6\u5e93");
        }
        return kb;
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
