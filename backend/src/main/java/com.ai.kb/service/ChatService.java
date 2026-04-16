package com.ai.kb.service;

import com.ai.kb.dto.ChatRequest;
import com.ai.kb.dto.ChatResponse;
import com.ai.kb.dto.ConversationDetailResponse;
import com.ai.kb.dto.ConversationMemoryUpdateRequest;
import com.ai.kb.dto.ConversationResponse;
import com.ai.kb.dto.MessageResponse;
import com.ai.kb.dto.RetrievalDebugRequest;
import com.ai.kb.dto.RetrievalDebugResponse;
import com.ai.kb.dto.RetrievalHitResponse;
import com.ai.kb.dto.SourceResponse;
import com.ai.kb.entity.Conversation;
import com.ai.kb.entity.LlmConfig;
import com.ai.kb.entity.Message;
import com.ai.kb.repository.ConversationRepository;
import com.ai.kb.repository.LlmConfigRepository;
import com.ai.kb.repository.MessageRepository;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MEMORY_TRIGGER_MESSAGE_COUNT = 4;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final LlmConfigRepository llmConfigRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_RETRIEVAL_DEBUG_TOP_K = 8;
    private static final int CHAT_SOURCE_LIMIT = 5;

    @Transactional
    public ChatResponse chat(ChatRequest request, String userId) {
        Conversation conversation = getOrCreateConversation(request, userId);

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole("user");
        userMessage.setContent(request.message());
        messageRepository.save(userMessage);

        String answerContent;
        String answerThinking = "";
        List<SourceResponse> sources;

        try {
            LlmConfig llmConfig = getEnabledConfig(userId);
            RetrievalResult retrievalResult = retrieveKnowledgeContext(request, llmConfig);

            String rewrittenQuery = llmService.rewriteQuery(request.message(), llmConfig);
            if (!rewrittenQuery.isBlank() && !rewrittenQuery.equals(request.message())) {
                RetrievalResult rewrittenResult = retrieveKnowledgeContext(
                        new ChatRequest(request.knowledgeBaseId(), rewrittenQuery, request.conversationId()),
                        llmConfig
                );
                if (rewrittenResult.sources().size() > retrievalResult.sources().size()) {
                    retrievalResult = rewrittenResult;
                }
            }

            String memoryContext = buildMemoryContext(conversation);
            String knowledgeContext = retrievalResult.context();
            String fullContext = joinContext(memoryContext, knowledgeContext);

            LlmService.GenerationResult result = llmService.chat(request.message(), fullContext, llmConfig);
            answerContent = result.content();
            answerThinking = result.thinking();
            sources = retrievalResult.sources();
        } catch (Exception e) {
            log.error("AI chat failed", e);
            answerContent = "AI 问答失败：" + e.getMessage();
            sources = Collections.emptyList();
        }

        Message aiMessage = new Message();
        aiMessage.setConversationId(conversation.getId());
        aiMessage.setRole("assistant");
        aiMessage.setContent(answerContent);
        aiMessage.setThinking(answerThinking);
        aiMessage.setSources(toJson(sources));
        aiMessage = messageRepository.save(aiMessage);

        conversation.setMessageCount(conversation.getMessageCount() + 2);
        updateConversationMemoryAndTitle(conversation, userId);
        conversationRepository.save(conversation);

        return new ChatResponse(
                conversation.getId(),
                aiMessage.getId(),
                conversation.getTitle(),
                answerContent,
                answerThinking,
                sources
        );
    }

    public List<ConversationResponse> getConversationList(String userId, String kbId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updatedAt").descending());
        Page<Conversation> convPage = conversationRepository.findByUserIdAndKnowledgeBaseId(userId, kbId, pageable);

        return convPage.getContent().stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());
    }

    public RetrievalDebugResponse debugRetrieval(RetrievalDebugRequest request, String userId) {
        if (request.knowledgeBaseId() == null || request.knowledgeBaseId().isBlank()) {
            throw new IllegalArgumentException("知识库不能为空");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("调试问题不能为空");
        }

        LlmConfig llmConfig = null;
        int topK = sanitizeTopK(request.topK());
        String originalQuery = request.message().trim();

        try {
            llmConfig = getEnabledConfig(userId);
        } catch (Exception e) {
            log.info("Retrieval debug will skip query rewrite because no enabled LLM config is available: {}", e.getMessage());
        }

        SearchOutcome originalOutcome = searchKnowledgeHits(originalQuery, request.knowledgeBaseId(), llmConfig, topK, topK);
        String rewrittenQuery = "";

        if (llmConfig != null) {
            try {
                rewrittenQuery = llmService.rewriteQuery(originalQuery, llmConfig);
            } catch (Exception e) {
                log.warn("Retrieval debug query rewrite failed, fallback to original query", e);
            }
        }

        SearchOutcome rewrittenOutcome = SearchOutcome.empty();
        String usedQuery = originalQuery;
        SearchOutcome finalOutcome = originalOutcome;

        if (rewrittenQuery != null) {
            rewrittenQuery = rewrittenQuery.trim();
        }

        if (rewrittenQuery != null && !rewrittenQuery.isBlank() && !rewrittenQuery.equals(originalQuery)) {
            rewrittenOutcome = searchKnowledgeHits(rewrittenQuery, request.knowledgeBaseId(), llmConfig, topK, topK);
            if (shouldUseRewritten(originalOutcome, rewrittenOutcome)) {
                usedQuery = rewrittenQuery;
                finalOutcome = rewrittenOutcome;
            }
        } else {
            rewrittenQuery = "";
        }

        return new RetrievalDebugResponse(
                originalQuery,
                rewrittenQuery,
                usedQuery,
                originalOutcome.hits(),
                rewrittenOutcome.hits(),
                finalOutcome.hits()
        );
    }

    public ConversationDetailResponse getConversationDetail(String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));

        List<MessageResponse> messageResponses = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getSessionSummary(),
                conversation.getSessionFacts(),
                messageResponses
        );
    }

    @Transactional
    public ConversationDetailResponse updateConversationMemory(
            String conversationId,
            ConversationMemoryUpdateRequest request,
            String userId
    ) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此会话");
        }

        String summary = request.sessionSummary() == null ? null : request.sessionSummary().trim();
        String facts = request.sessionFacts() == null ? null : request.sessionFacts().trim();

        if (summary != null && summary.length() > 4000) {
            throw new IllegalArgumentException("会话摘要超过长度限制");
        }
        if (facts != null && facts.length() > 4000) {
            throw new IllegalArgumentException("会话事实超过长度限制");
        }

        conversation.setSessionSummary(summary == null || summary.isBlank() ? null : summary);
        conversation.setSessionFacts(facts == null || facts.isBlank() ? null : facts);
        conversationRepository.save(conversation);

        return getConversationDetail(conversationId);
    }

    @Transactional
    public void deleteConversation(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此会话");
        }
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    public ExportPayload exportConversation(String conversationId, String format) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        List<MessageResponse> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        String markdown = buildConversationMarkdown(conversation, messages);
        if ("pdf".equalsIgnoreCase(format)) {
            return new ExportPayload(
                    safeFileName(conversation.getTitle()) + ".pdf",
                    renderPdf(markdown),
                    "application/pdf"
            );
        }

        return new ExportPayload(
                safeFileName(conversation.getTitle()) + ".md",
                markdown.getBytes(StandardCharsets.UTF_8),
                "text/markdown; charset=UTF-8"
        );
    }

    private Conversation getOrCreateConversation(ChatRequest request, String userId) {
        if (request.conversationId() == null || request.conversationId().isEmpty()) {
            Conversation conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setKnowledgeBaseId(request.knowledgeBaseId());
            conversation.setTitle(generateFallbackTitle(request.message()));
            conversation.setMessageCount(0);
            return conversationRepository.save(conversation);
        }

        return conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
    }

    private LlmConfig getEnabledConfig(String userId) {
        LlmConfig llmConfig = llmConfigRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先在设置中配置并启用 LLM API，才能使用 AI 问答功能。"));
        if (!Boolean.TRUE.equals(llmConfig.getIsEnabled())) {
            throw new IllegalArgumentException("请先在设置中配置并启用 LLM API，才能使用 AI 问答功能。");
        }
        return llmConfig;
    }

    private RetrievalResult retrieveKnowledgeContext(ChatRequest request, LlmConfig llmConfig) {
        if (request.knowledgeBaseId() == null || request.knowledgeBaseId().isBlank()) {
            return new RetrievalResult("", Collections.emptyList());
        }

        try {
            SearchOutcome outcome = searchKnowledgeHits(
                    request.message(),
                    request.knowledgeBaseId(),
                    llmConfig,
                    DEFAULT_RETRIEVAL_DEBUG_TOP_K,
                    CHAT_SOURCE_LIMIT
            );
            return new RetrievalResult(outcome.context(), outcome.sources());
        } catch (Exception e) {
            log.warn("Knowledge retrieval failed, fallback to general answer", e);
            return new RetrievalResult("", Collections.emptyList());
        }
    }

    private SearchOutcome searchKnowledgeHits(
            String query,
            String knowledgeBaseId,
            LlmConfig llmConfig,
            int searchTopK,
            int displayLimit
    ) {
        float[] queryEmbedding = embeddingService.getEmbeddingWithConfig(query, llmConfig);
        List<VectorStoreService.VectorSearchResult> searchResults =
                vectorStoreService.searchSimilar(queryEmbedding, knowledgeBaseId, searchTopK);

        List<VectorStoreService.VectorSearchResult> ordered = searchResults.stream()
                .sorted((a, b) -> {
                    boolean aSummary = "summary".equalsIgnoreCase(a.getChunkType());
                    boolean bSummary = "summary".equalsIgnoreCase(b.getChunkType());
                    if (aSummary != bSummary) {
                        return Boolean.compare(bSummary, aSummary);
                    }
                    return Double.compare(b.getScore(), a.getScore());
                })
                .limit(displayLimit)
                .toList();

        StringBuilder contextBuilder = new StringBuilder();
        List<SourceResponse> sources = new ArrayList<>();
        List<RetrievalHitResponse> hits = new ArrayList<>();

        for (VectorStoreService.VectorSearchResult result : ordered) {
            contextBuilder.append("[")
                    .append(result.getChunkType())
                    .append("] ")
                    .append(result.getContent())
                    .append("\n\n");
            sources.add(new SourceResponse(
                    result.getDocumentId(),
                    result.getDocumentTitle(),
                    result.getContent(),
                    result.getScore()
            ));
            hits.add(new RetrievalHitResponse(
                    result.getId(),
                    result.getDocumentId(),
                    result.getDocumentTitle(),
                    result.getContent(),
                    result.getChunkType(),
                    result.getChunkIndex(),
                    result.getScore()
            ));
        }

        return new SearchOutcome(contextBuilder.toString(), sources, hits);
    }

    private boolean shouldUseRewritten(SearchOutcome originalOutcome, SearchOutcome rewrittenOutcome) {
        if (rewrittenOutcome.hits().isEmpty()) {
            return false;
        }
        if (originalOutcome.hits().isEmpty()) {
            return true;
        }
        if (rewrittenOutcome.hits().size() > originalOutcome.hits().size()) {
            return true;
        }
        return bestScore(rewrittenOutcome.hits()) > bestScore(originalOutcome.hits()) + 0.03d;
    }

    private double bestScore(List<RetrievalHitResponse> hits) {
        return hits.stream().mapToDouble(RetrievalHitResponse::score).max().orElse(0d);
    }

    private int sanitizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_RETRIEVAL_DEBUG_TOP_K;
        }
        return Math.max(1, Math.min(topK, 20));
    }

    private String buildMemoryContext(Conversation conversation) {
        List<String> parts = new ArrayList<>();
        if (conversation.getSessionSummary() != null && !conversation.getSessionSummary().isBlank()) {
            parts.add("Conversation summary:\n" + conversation.getSessionSummary());
        }
        if (conversation.getSessionFacts() != null && !conversation.getSessionFacts().isBlank()) {
            parts.add("Conversation facts:\n" + conversation.getSessionFacts());
        }
        return String.join("\n\n", parts);
    }

    private String joinContext(String memoryContext, String knowledgeContext) {
        if ((memoryContext == null || memoryContext.isBlank()) && (knowledgeContext == null || knowledgeContext.isBlank())) {
            return "";
        }
        if (memoryContext == null || memoryContext.isBlank()) {
            return knowledgeContext;
        }
        if (knowledgeContext == null || knowledgeContext.isBlank()) {
            return memoryContext;
        }
        return memoryContext + "\n\n" + knowledgeContext;
    }

    private void updateConversationMemoryAndTitle(Conversation conversation, String userId) {
        try {
            LlmConfig config = getEnabledConfig(userId);
            List<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            if (allMessages.size() >= MEMORY_TRIGGER_MESSAGE_COUNT) {
                List<String> recentMessages = allMessages.stream()
                        .skip(Math.max(0, allMessages.size() - 8L))
                        .map(msg -> msg.getRole() + ": " + msg.getContent())
                        .toList();
                LlmService.MemoryResult memory = llmService.buildConversationMemory(
                        conversation.getSessionSummary(),
                        conversation.getSessionFacts(),
                        recentMessages,
                        config
                );
                conversation.setSessionSummary(memory.summary());
                conversation.setSessionFacts(memory.facts());
            }

            if (!Boolean.TRUE.equals(conversation.getTitleGenerated()) && allMessages.size() >= 2) {
                List<String> seed = allMessages.stream()
                        .limit(4)
                        .map(msg -> msg.getRole() + ": " + msg.getContent())
                        .toList();
                String generatedTitle = llmService.generateConversationTitle(seed, config);
                if (generatedTitle != null && !generatedTitle.isBlank()) {
                    conversation.setTitle(generatedTitle.strip());
                    conversation.setTitleGenerated(true);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update conversation memory/title", e);
        }
    }

    private String generateFallbackTitle(String firstMessage) {
        String message = firstMessage == null ? "新对话" : firstMessage.trim();
        if (message.isEmpty()) {
            return "新对话";
        }
        if (message.length() > 18) {
            return message.substring(0, 18) + "...";
        }
        return message;
    }

    private ConversationResponse toConversationResponse(Conversation conv) {
        return new ConversationResponse(
                conv.getId(),
                conv.getTitle(),
                conv.getMessageCount(),
                conv.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(Message msg) {
        return new MessageResponse(
                msg.getId(),
                msg.getRole(),
                msg.getContent(),
                msg.getThinking(),
                parseSources(msg.getSources()),
                msg.getCreatedAt()
        );
    }

    private String buildConversationMarkdown(Conversation conversation, List<MessageResponse> messages) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(conversation.getTitle()).append("\n\n");
        if (conversation.getSessionSummary() != null && !conversation.getSessionSummary().isBlank()) {
            markdown.append("## 会话摘要\n\n").append(conversation.getSessionSummary()).append("\n\n");
        }
        if (conversation.getSessionFacts() != null && !conversation.getSessionFacts().isBlank()) {
            markdown.append("## 会话记忆\n\n").append(conversation.getSessionFacts()).append("\n\n");
        }
        markdown.append("## 消息记录\n\n");
        for (MessageResponse message : messages) {
            markdown.append("### ").append("user".equals(message.role()) ? "用户" : "AI").append("\n\n");
            markdown.append(message.content()).append("\n\n");
            if (message.thinking() != null && !message.thinking().isBlank()) {
                markdown.append("**思考过程**\n\n").append(message.thinking()).append("\n\n");
            }
            if (message.sources() != null && !message.sources().isEmpty()) {
                markdown.append("**引用来源**\n");
                for (SourceResponse source : message.sources()) {
                    markdown.append("- ").append(source.documentTitle()).append("\n");
                }
                markdown.append("\n");
            }
        }
        return markdown.toString();
    }

    private byte[] renderPdf(String markdown) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.setLeading(14f);
                contentStream.newLineAtOffset(40, 780);
                for (String line : markdown.split("\n")) {
                    String safeLine = line.replace("\t", "    ");
                    contentStream.showText(safeLine.length() > 100 ? safeLine.substring(0, 100) : safeLine);
                    contentStream.newLine();
                }
                contentStream.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出 PDF 失败: " + e.getMessage(), e);
        }
    }

    private String safeFileName(String title) {
        String value = (title == null || title.isBlank()) ? "conversation" : title;
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String toJson(List<SourceResponse> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JacksonException e) {
            return "[]";
        }
    }

    private List<SourceResponse> parseSources(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SourceResponse.class)
            );
        } catch (JacksonException e) {
            return Collections.emptyList();
        }
    }

    private record RetrievalResult(String context, List<SourceResponse> sources) {
    }

    private record SearchOutcome(
            String context,
            List<SourceResponse> sources,
            List<RetrievalHitResponse> hits
    ) {
        private static SearchOutcome empty() {
            return new SearchOutcome("", Collections.emptyList(), Collections.emptyList());
        }
    }

    public record ExportPayload(String fileName, byte[] content, String contentType) {
    }
}
