package com.ai.kb.service;

import com.ai.kb.dto.DocumentListResponse;
import com.ai.kb.dto.DocumentResponse;
import com.ai.kb.dto.DocumentSummaryRequest;
import com.ai.kb.dto.DocumentTaskListResponse;
import com.ai.kb.dto.DocumentTaskResponse;
import com.ai.kb.dto.DocumentUpdateRequest;
import com.ai.kb.dto.DocumentUploadResponse;
import com.ai.kb.entity.Category;
import com.ai.kb.entity.Document;
import com.ai.kb.entity.DocumentChunk;
import com.ai.kb.entity.DocumentTask;
import com.ai.kb.entity.KnowledgeBase;
import com.ai.kb.entity.LlmConfig;
import com.ai.kb.repository.CategoryRepository;
import com.ai.kb.repository.DocumentChunkRepository;
import com.ai.kb.repository.DocumentRepository;
import com.ai.kb.repository.DocumentTaskRepository;
import com.ai.kb.repository.KnowledgeBaseRepository;
import com.ai.kb.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    public static final String SUMMARY_MODE_AI = "AI_GENERATED";
    public static final String SUMMARY_MODE_HEURISTIC = "HEURISTIC";
    public static final String SUMMARY_MODE_MANUAL = "MANUAL_EDITED";
    public static final String SUMMARY_MODE_EMPTY = "EMPTY";
    public static final String STAGE_UPLOADED = "UPLOADED";
    public static final String STAGE_PARSING = "PARSING";
    public static final String STAGE_SUMMARIZING = "SUMMARIZING";
    public static final String STAGE_INDEXING = "INDEXING";
    public static final String STAGE_COMPLETED = "COMPLETED";
    public static final String STAGE_FAILED = "FAILED";
    public static final String TASK_TYPE_INGEST = "INGEST";
    public static final String TASK_TYPE_SUMMARY = "SUMMARY";
    public static final String TASK_TYPE_RETRY = "RETRY";

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    private static final int MAX_CONTENT_LENGTH = -1;
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final List<String> DIRECT_TEXT_TYPES = List.of(
            "md", "markdown", "txt", "csv", "json", "xml", "yaml", "yml", "html", "htm", "java", "js", "ts", "vue"
    );

    private final DocumentRepository documentRepository;
    private final DocumentTaskRepository documentTaskRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final CategoryRepository categoryRepository;
    private final LlmConfigRepository llmConfigRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public DocumentUploadResponse upload(MultipartFile file, String kbId, String categoryId, String userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("鐭ヨ瘑搴撲笉瀛樺湪"));
        validateCategory(kbId, categoryId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("鏂囦欢涓嶈兘涓虹┖");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        long fileSize = file.getSize();

        Path uploadPath = Paths.get(UPLOAD_DIR, kbId);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("鍒涘缓涓婁紶鐩綍澶辫触", e);
        }

        String fileName = UUID.randomUUID() + "." + fileType;
        Path filePath = uploadPath.resolve(fileName);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("淇濆瓨鏂囦欢澶辫触", e);
        }

        Document document = new Document();
        document.setKnowledgeBaseId(kbId);
        document.setCategoryId(categoryId);
        document.setUserId(userId);
        document.setTitle(originalFilename);
        document.setFilePath(filePath.toString());
        document.setFileType(fileType);
        document.setFileSize(fileSize);
        document.setStatus("UPLOADED");
        document.setProcessingStage(STAGE_UPLOADED);
        document.setLastError(null);
        Document saved = documentRepository.save(document);
        DocumentTask task = createTask(saved, TASK_TYPE_INGEST, null, STAGE_UPLOADED, "PROCESSING");

        updateKnowledgeBaseDocumentCount(kb.getId());
        if (categoryId != null && !categoryId.isBlank()) {
            updateCategoryDocumentCount(categoryId);
        }

        CompletableFuture.runAsync(() -> processDocument(saved.getId(), task.getId()));
        return new DocumentUploadResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getStatus(),
                saved.getSummaryContent(),
                saved.getSummaryType()
        );
    }

    @Transactional
    public void processDocument(String documentId) {
        processDocument(documentId, null);
    }

    @Transactional
    public void processDocument(String documentId, String taskId) {
        transactionTemplate.executeWithoutResult(status -> {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
            try {
                document.setStatus("PROCESSING");
                document.setProcessingStage(STAGE_PARSING);
                document.setLastError(null);
                documentRepository.save(document);
                updateTask(taskId, "PROCESSING", STAGE_PARSING, null);

                String parsedContent = parseDocument(document.getFilePath(), document.getFileType());
                document.setParsedContent(parsedContent);

                LlmConfig llmConfig = getDefaultEnabledLlmConfig(document.getUserId());
                document.setProcessingStage(STAGE_SUMMARIZING);
                documentRepository.save(document);
                updateTask(taskId, "PROCESSING", STAGE_SUMMARIZING, null);
                SummaryResult summaryResult = generateSummary(document, parsedContent, SUMMARY_MODE_AI, llmConfig);
                applySummaryResult(document, summaryResult);
                document.setProcessingStage(STAGE_INDEXING);
                documentRepository.save(document);
                updateTask(taskId, "PROCESSING", STAGE_INDEXING, null);

                reindexDocument(document, llmConfig);
                document.setStatus("SUMMARIZED");
                document.setProcessingStage(STAGE_COMPLETED);
                document.setLastError(null);
                documentRepository.save(document);
                completeTask(taskId, STAGE_COMPLETED);
            } catch (Exception e) {
                log.error("Failed to process document {}", documentId, e);
                document.setStatus("FAILED");
                document.setProcessingStage(STAGE_FAILED);
                document.setLastError(buildErrorMessage(e));
                documentRepository.save(document);
                failTask(taskId, buildErrorMessage(e));
            }
        });
    }

    public DocumentListResponse getList(String kbId, String categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Document> docPage = (categoryId != null && !categoryId.isEmpty())
                ? documentRepository.findByKnowledgeBaseIdAndCategoryId(kbId, categoryId, pageable)
                : documentRepository.findByKnowledgeBaseId(kbId, pageable);

        List<DocumentResponse> list = docPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new DocumentListResponse(list, docPage.getTotalElements(), page, size);
    }

    public DocumentResponse getById(String documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        ensurePreviewContent(doc);
        return toResponse(doc);
    }

    @Transactional
    public DocumentResponse requestSummaryGeneration(String documentId, DocumentSummaryRequest request, String userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此文档");
        }
        if (request.content() != null) {
            document.setParsedContent(normalizeParsedContent(request.content(), document.getFileType()));
        }
        if (document.getParsedContent() == null || document.getParsedContent().isBlank()) {
            throw new IllegalArgumentException("当前没有可用于生成总结的解析文本");
        }

        String requestedMode = normalizeSummaryMode(request.summaryMode());
        if (SUMMARY_MODE_MANUAL.equals(requestedMode)) {
            throw new IllegalArgumentException("人工编辑摘要请直接在编辑框中保存");
        }

        document.setStatus("PROCESSING");
        document.setProcessingStage(STAGE_SUMMARIZING);
        document.setLastError(null);
        documentRepository.save(document);
        DocumentTask task = createTask(document, TASK_TYPE_SUMMARY, requestedMode, STAGE_SUMMARIZING, "PROCESSING");

        final String mode = requestedMode != null ? requestedMode : SUMMARY_MODE_AI;
        CompletableFuture.runAsync(() -> generateSummaryAsync(document.getId(), mode, userId, task.getId()));
        return toResponse(document);
    }

    @Transactional
    public DocumentResponse retryProcessing(String documentId, String userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此文档");
        }
        if ("PROCESSING".equals(document.getStatus()) || "UPLOADED".equals(document.getStatus())) {
            return toResponse(document);
        }

        document.setStatus("PROCESSING");
        document.setProcessingStage(document.getParsedContent() == null || document.getParsedContent().isBlank()
                ? STAGE_PARSING
                : STAGE_SUMMARIZING);
        document.setLastError(null);
        documentRepository.save(document);
        DocumentTask task = createTask(document, TASK_TYPE_RETRY, document.getSummaryType(), document.getProcessingStage(), "PROCESSING");

        if (document.getParsedContent() == null || document.getParsedContent().isBlank()) {
            CompletableFuture.runAsync(() -> processDocument(document.getId(), task.getId()));
        } else {
            String summaryMode = document.getSummaryType();
            if (summaryMode == null || summaryMode.isBlank() || SUMMARY_MODE_EMPTY.equals(summaryMode)) {
                summaryMode = SUMMARY_MODE_AI;
            }
            final String targetMode = summaryMode;
            CompletableFuture.runAsync(() -> generateSummaryAsync(document.getId(), targetMode, userId, task.getId()));
        }
        return toResponse(document);
    }

    @Transactional
    public DocumentResponse updateContent(String documentId, DocumentUpdateRequest request, String userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此文档");
        }

        if (request.content() != null) {
            document.setParsedContent(normalizeParsedContent(request.content(), document.getFileType()));
        }

        String requestedMode = normalizeSummaryMode(request.summaryMode());
        boolean shouldRegenerate = Boolean.TRUE.equals(request.regenerateSummary());
        if (shouldRegenerate || requestedMode != null) {
            handleSummaryUpdate(document, request, requestedMode, userId, shouldRegenerate);
        } else if (request.summaryContent() != null) {
            if (request.summaryContent().isBlank()) {
                document.setSummaryContent(buildFallbackSummary(document.getTitle(), document.getFileType(), document.getParsedContent()));
                document.setSummaryType(SUMMARY_MODE_HEURISTIC);
            } else {
                document.setSummaryContent(request.summaryContent());
                document.setSummaryType(SUMMARY_MODE_MANUAL);
            }
            document.setSummaryUpdatedAt(LocalDateTime.now());
        }

        if (document.getSummaryContent() == null || document.getSummaryContent().isBlank()) {
            SummaryResult fallback = generateSummary(document, document.getParsedContent(), SUMMARY_MODE_HEURISTIC, getDefaultEnabledLlmConfig(userId));
            applySummaryResult(document, fallback);
        }

        document.setStatus("SUMMARIZED");
        document.setProcessingStage(STAGE_INDEXING);
        document.setLastError(null);
        documentRepository.save(document);

        LlmConfig llmConfig = getDefaultEnabledLlmConfig(userId);
        reindexDocument(document, llmConfig);
        document.setProcessingStage(STAGE_COMPLETED);
        documentRepository.save(document);
        return toResponse(document);
    }

    private void generateSummaryAsync(String documentId, String summaryMode, String userId) {
        generateSummaryAsync(documentId, summaryMode, userId, null);
    }

    private void generateSummaryAsync(String documentId, String summaryMode, String userId, String taskId) {
        transactionTemplate.executeWithoutResult(status -> {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
            try {
                LlmConfig llmConfig = getDefaultEnabledLlmConfig(userId);
                document.setProcessingStage(STAGE_SUMMARIZING);
                documentRepository.save(document);
                updateTask(taskId, "PROCESSING", STAGE_SUMMARIZING, null);
                SummaryResult result = generateSummary(document, document.getParsedContent(), summaryMode, llmConfig);
                applySummaryResult(document, result);
                document.setProcessingStage(STAGE_INDEXING);
                document.setLastError(null);
                documentRepository.save(document);
                updateTask(taskId, "PROCESSING", STAGE_INDEXING, null);
                reindexDocument(document, llmConfig);
                document.setStatus("SUMMARIZED");
                document.setProcessingStage(STAGE_COMPLETED);
                document.setLastError(null);
                documentRepository.save(document);
                completeTask(taskId, STAGE_COMPLETED);
            } catch (Exception e) {
                log.error("Failed to generate summary asynchronously for document {}", documentId, e);
                document.setStatus("FAILED");
                document.setProcessingStage(STAGE_FAILED);
                document.setLastError(buildErrorMessage(e));
                documentRepository.save(document);
                failTask(taskId, buildErrorMessage(e));
            }
        });
    }

    @Transactional
    public void delete(String documentId, String userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此文档");
        }

        deleteChunksAndVectors(documentId);
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (IOException e) {
            log.warn("鍒犻櫎鏂囦欢澶辫触: {}", e.getMessage());
        }

        String oldCategoryId = doc.getCategoryId();
        documentRepository.delete(doc);
        updateKnowledgeBaseDocumentCount(doc.getKnowledgeBaseId());
        if (oldCategoryId != null) {
            updateCategoryDocumentCount(oldCategoryId);
        }
    }

    @Transactional
    public void setCategory(String documentId, String categoryId, String userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此文档");
        }

        String oldCategoryId = doc.getCategoryId();
        validateCategory(doc.getKnowledgeBaseId(), categoryId);
        doc.setCategoryId(categoryId == null || categoryId.isBlank() ? null : categoryId);
        documentRepository.save(doc);

        if (oldCategoryId != null) {
            updateCategoryDocumentCount(oldCategoryId);
        }
        if (categoryId != null && !categoryId.isBlank()) {
            updateCategoryDocumentCount(categoryId);
        }
    }

    private void handleSummaryUpdate(Document document, DocumentUpdateRequest request, String requestedMode, String userId, boolean shouldRegenerate) {
        String targetMode = requestedMode != null ? requestedMode : document.getSummaryType();
        if (targetMode == null || targetMode.isBlank()) {
            targetMode = SUMMARY_MODE_MANUAL;
        }

        if (SUMMARY_MODE_MANUAL.equals(targetMode) && !shouldRegenerate) {
            String manualSummary = request.summaryContent();
            if (manualSummary == null || manualSummary.isBlank()) {
                throw new IllegalArgumentException("浜哄伐缂栬緫鎽樿涓嶈兘涓虹┖");
            }
            document.setSummaryContent(manualSummary);
            document.setSummaryType(SUMMARY_MODE_MANUAL);
            document.setSummaryUpdatedAt(LocalDateTime.now());
            return;
        }

        if (document.getParsedContent() == null || document.getParsedContent().isBlank()) {
            throw new IllegalArgumentException("褰撳墠娌℃湁鍙敤浜庣敓鎴愭憳瑕佺殑瑙ｆ瀽鏂囨湰");
        }

        LlmConfig llmConfig = getDefaultEnabledLlmConfig(userId);
        SummaryResult result = generateSummary(document, document.getParsedContent(), targetMode, llmConfig);
        applySummaryResult(document, result);
    }

    private SummaryResult generateSummary(Document document, String parsedContent, String requestedMode, LlmConfig llmConfig) {
        String normalizedMode = normalizeSummaryMode(requestedMode);
        if (parsedContent == null || parsedContent.isBlank()) {
            return new SummaryResult(buildEmptySummary(document.getTitle(), document.getFileType()), SUMMARY_MODE_EMPTY);
        }

        if (SUMMARY_MODE_HEURISTIC.equals(normalizedMode)) {
            return new SummaryResult(buildFallbackSummary(document.getTitle(), document.getFileType(), parsedContent), SUMMARY_MODE_HEURISTIC);
        }

        if (SUMMARY_MODE_MANUAL.equals(normalizedMode)) {
            return new SummaryResult(document.getSummaryContent(), SUMMARY_MODE_MANUAL);
        }

        if (llmConfig == null || !Boolean.TRUE.equals(llmConfig.getIsEnabled())) {
            if (SUMMARY_MODE_AI.equals(normalizedMode)) {
                throw new IllegalArgumentException("褰撳墠鏈厤缃彲鐢ㄧ殑 LLM锛屾棤娉曠敓鎴?AI 鎬荤粨");
            }
            return new SummaryResult(buildFallbackSummary(document.getTitle(), document.getFileType(), parsedContent), SUMMARY_MODE_HEURISTIC);
        }

        try {
            String content = llmService.summarizeDocument(document.getTitle(), parsedContent, llmConfig);
            if (content == null || content.isBlank()) {
                if (SUMMARY_MODE_AI.equals(normalizedMode)) {
                    throw new IllegalArgumentException("AI 鎬荤粨鐢熸垚澶辫触锛岃绋嶅悗閲嶈瘯");
                }
                return new SummaryResult(buildFallbackSummary(document.getTitle(), document.getFileType(), parsedContent), SUMMARY_MODE_HEURISTIC);
            }
            return new SummaryResult(content, SUMMARY_MODE_AI);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Document summary generation failed", e);
            if (SUMMARY_MODE_AI.equals(normalizedMode)) {
                throw new IllegalArgumentException("AI 鎬荤粨鐢熸垚澶辫触: " + e.getMessage());
            }
            return new SummaryResult(buildFallbackSummary(document.getTitle(), document.getFileType(), parsedContent), SUMMARY_MODE_HEURISTIC);
        }
    }

    private void applySummaryResult(Document document, SummaryResult result) {
        document.setSummaryContent(result.content());
        document.setSummaryType(result.type());
        document.setSummaryUpdatedAt(LocalDateTime.now());
    }

    private void validateCategory(String kbId, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        if (!category.getKnowledgeBaseId().equals(kbId)) {
            throw new IllegalArgumentException("鍒嗙被涓嶅睘浜庡綋鍓嶇煡璇嗗簱");
        }
    }

    private void ensurePreviewContent(Document document) {
        if (!isDirectTextType(document.getFileType())) {
            return;
        }
        if (document.getParsedContent() != null && !document.getParsedContent().isBlank()) {
            return;
        }
        try {
            String content = parseDocument(document.getFilePath(), document.getFileType());
            if (content == null || content.isBlank()) {
                return;
            }
            document.setParsedContent(content);
            if (document.getSummaryContent() == null || document.getSummaryContent().isBlank()) {
                SummaryResult fallback = generateSummary(document, content, SUMMARY_MODE_HEURISTIC, null);
                applySummaryResult(document, fallback);
            }
            if ("UPLOADED".equals(document.getStatus()) || "FAILED".equals(document.getStatus())) {
                document.setStatus("SUMMARIZED");
            }
            document.setProcessingStage(STAGE_COMPLETED);
            document.setLastError(null);
            documentRepository.save(document);
        } catch (Exception e) {
            log.warn("Failed to hydrate preview content for document {}", document.getId(), e);
        }
    }

    private void reindexDocument(Document document, LlmConfig llmConfig) {
        deleteChunksAndVectors(document.getId());

        List<ChunkPayload> chunkPayloads = new ArrayList<>();
        List<String> rawChunks = chunkContentWithOverlap(document.getParsedContent(), CHUNK_SIZE, CHUNK_OVERLAP);
        for (int i = 0; i < rawChunks.size(); i++) {
            chunkPayloads.add(new ChunkPayload(i, "raw", rawChunks.get(i)));
        }

        List<String> summaryChunks = chunkContentWithOverlap(document.getSummaryContent(), CHUNK_SIZE, CHUNK_OVERLAP);
        for (int i = 0; i < summaryChunks.size(); i++) {
            chunkPayloads.add(new ChunkPayload(1000 + i, "summary", summaryChunks.get(i)));
        }

        List<DocumentChunk> chunks = chunkPayloads.stream().map(payload -> {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setChunkIndex(payload.index());
            chunk.setChunkType(payload.type());
            chunk.setContent(payload.content());
            chunk.setCharCount(payload.content().length());
            return chunk;
        }).collect(Collectors.toList());

        List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunks);
        if (llmConfig == null || !Boolean.TRUE.equals(llmConfig.getIsEnabled())) {
            return;
        }

        List<String> contents = savedChunks.stream().map(DocumentChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.getEmbeddings(contents, llmConfig);
        List<String> chunkIds = savedChunks.stream().map(DocumentChunk::getId).toList();
        List<Map<String, Object>> metadataList = savedChunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("documentId", document.getId());
                    metadata.put("documentTitle", document.getTitle());
                    metadata.put("knowledgeBaseId", document.getKnowledgeBaseId());
                    metadata.put("chunkIndex", chunk.getChunkIndex());
                    metadata.put("chunkType", chunk.getChunkType());
                    metadata.put("summaryType", document.getSummaryType());
                    return metadata;
                }).toList();

        List<String> vectorIds = vectorStoreService.storeVectors(chunkIds, contents, embeddings, metadataList);
        for (int i = 0; i < savedChunks.size() && i < vectorIds.size(); i++) {
            savedChunks.get(i).setVectorId(vectorIds.get(i));
        }
        documentChunkRepository.saveAll(savedChunks);
    }

    private void deleteChunksAndVectors(String documentId) {
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        List<String> vectorIds = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        vectorStoreService.deleteVectors(vectorIds);
        documentChunkRepository.deleteByDocumentId(documentId);
    }

    private String parseDocument(String filePath, String fileType) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("鏂囦欢涓嶅瓨鍦? " + filePath);
            }

            if (isDirectTextType(fileType)) {
                return normalizeParsedContent(Files.readString(file.toPath(), StandardCharsets.UTF_8), fileType);
            }

            try (InputStream stream = new FileInputStream(file)) {
                AutoDetectParser parser = new AutoDetectParser();
                BodyContentHandler handler = new BodyContentHandler(MAX_CONTENT_LENGTH);
                Metadata metadata = new Metadata();
                ParseContext context = new ParseContext();
                parser.parse(stream, handler, metadata, context);
                return normalizeParsedContent(handler.toString(), fileType);
            }
        } catch (Exception e) {
            throw new RuntimeException("瑙ｆ瀽鏂囨。澶辫触: " + e.getMessage(), e);
        }
    }

    private String buildFallbackSummary(String title, String fileType, String parsedContent) {
        String excerpt = parsedContent == null ? "" : parsedContent.trim();
        if (excerpt.length() > 1200) {
            excerpt = excerpt.substring(0, 1200) + "...";
        }
        return """
                ## 鏂囨。姒傝

                - 鏍囬锛?s
                - 绫诲瀷锛?s
                - 鎬荤粨鏂瑰紡锛氬惎鍙戝紡鎽樿

                ## 鏍稿績鍐呭

                %s
                """.formatted(title, safeText(fileType).toUpperCase(Locale.ROOT), excerpt);
    }

    private String buildEmptySummary(String title, String fileType) {
        return """
                ## 鏂囨。姒傝

                - 鏍囬锛?s
                - 绫诲瀷锛?s
                - 鎬荤粨鏂瑰紡锛氬緟琛ュ厖

                ## 褰撳墠鐘舵€?
                鏆傛椂娌℃湁鍙敤浜庣敓鎴愭€荤粨鐨勮В鏋愬唴瀹广€備綘鍙互鍏堣ˉ鍏呰В鏋愭枃鏈紝鍐嶉€夋嫨鎬荤粨鏂瑰紡閲嶆柊鐢熸垚銆?                """.formatted(title, safeText(fileType).toUpperCase(Locale.ROOT));
    }

    private String normalizeParsedContent(String rawContent, String fileType) {
        if (rawContent == null) {
            return "";
        }

        String normalized = rawContent.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replace('\u0000', ' ');
        if (isMarkdownType(fileType)) {
            normalized = normalized.replaceAll("[\\x0B\\f]+", " ");
            normalized = normalized.replaceAll("\\n{4,}", "\n\n\n");
            return normalized.trim();
        }
        normalized = normalized.replaceAll("[\\t\\x0B\\f]+", " ");
        normalized = normalized.replaceAll("[ ]{2,}", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private boolean isDirectTextType(String fileType) {
        if (fileType == null) {
            return false;
        }
        return DIRECT_TEXT_TYPES.contains(fileType.toLowerCase(Locale.ROOT));
    }

    private boolean isMarkdownType(String fileType) {
        if (fileType == null) {
            return false;
        }
        String normalizedType = fileType.toLowerCase(Locale.ROOT);
        return "md".equals(normalizedType) || "markdown".equals(normalizedType);
    }

    private String normalizeSummaryMode(String summaryMode) {
        if (summaryMode == null || summaryMode.isBlank()) {
            return null;
        }
        String normalized = summaryMode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case SUMMARY_MODE_AI, SUMMARY_MODE_HEURISTIC, SUMMARY_MODE_MANUAL, SUMMARY_MODE_EMPTY -> normalized;
            default -> throw new IllegalArgumentException("涓嶆敮鎸佺殑鎬荤粨鏂瑰紡: " + summaryMode);
        };
    }

    private LlmConfig getDefaultEnabledLlmConfig(String userId) {
        return llmConfigRepository.findByUserIdAndIsDefaultTrue(userId)
                .filter(config -> Boolean.TRUE.equals(config.getIsEnabled()))
                .orElse(null);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private List<String> chunkContentWithOverlap(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }
        if (content.length() <= chunkSize) {
            chunks.add(content);
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            int lastPeriod = content.lastIndexOf('.', end);
            int lastNewline = content.lastIndexOf('\n', end);
            int breakPoint = Math.max(lastPeriod, lastNewline);
            if (breakPoint > start + chunkSize / 2) {
                end = breakPoint + 1;
            }
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            int nextStart = Math.max(end - overlap, 0);
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return chunks;
    }

    private void updateKnowledgeBaseDocumentCount(String kbId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
        if (kb != null) {
            kb.setDocumentCount((int) documentRepository.countByKnowledgeBaseId(kbId));
            knowledgeBaseRepository.save(kb);
        }
    }

    private void updateCategoryDocumentCount(String categoryId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            long count = categoryRepository.countDocumentsByCategoryId(categoryId);
            category.setDocumentCount((int) count);
            categoryRepository.save(category);
        }
    }

    private DocumentResponse toResponse(Document doc) {
        String categoryName = null;
        if (doc.getCategoryId() != null) {
            Category category = categoryRepository.findById(doc.getCategoryId()).orElse(null);
            if (category != null) {
                categoryName = category.getName();
            }
        }

        return new DocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getStatus(),
                doc.getProcessingStage(),
                doc.getCategoryId(),
                categoryName,
                doc.getCreatedAt(),
                doc.getParsedContent(),
                doc.getSummaryContent(),
                doc.getSummaryType(),
                doc.getSummaryUpdatedAt(),
                doc.getLastError()
        );
    }

    public DocumentTaskListResponse getTasks(String knowledgeBaseId, String documentId, String userId) {
        List<DocumentTask> tasks = (documentId == null || documentId.isBlank())
                ? documentTaskRepository.findTop50ByUserIdAndKnowledgeBaseIdOrderByCreatedAtDesc(userId, knowledgeBaseId)
                : documentTaskRepository.findTop50ByUserIdAndKnowledgeBaseIdAndDocumentIdOrderByCreatedAtDesc(userId, knowledgeBaseId, documentId);
        return new DocumentTaskListResponse(tasks.stream().map(this::toTaskResponse).toList());
    }

    private DocumentTask createTask(Document document, String taskType, String summaryMode, String stage, String status) {
        DocumentTask task = new DocumentTask();
        task.setDocumentId(document.getId());
        task.setKnowledgeBaseId(document.getKnowledgeBaseId());
        task.setUserId(document.getUserId());
        task.setDocumentTitle(document.getTitle());
        task.setTaskType(taskType);
        task.setStatus(status);
        task.setProcessingStage(stage);
        task.setSummaryMode(summaryMode);
        task.setErrorMessage(null);
        task.setCompletedAt(null);
        return documentTaskRepository.save(task);
    }

    private void updateTask(String taskId, String status, String stage, String errorMessage) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        documentTaskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            task.setProcessingStage(stage);
            task.setErrorMessage(errorMessage);
            if (!"PROCESSING".equals(status)) {
                task.setCompletedAt(LocalDateTime.now());
            }
            documentTaskRepository.save(task);
        });
    }

    private void completeTask(String taskId, String stage) {
        updateTask(taskId, "SUCCESS", stage, null);
    }

    private void failTask(String taskId, String errorMessage) {
        updateTask(taskId, "FAILED", STAGE_FAILED, errorMessage);
    }

    private DocumentTaskResponse toTaskResponse(DocumentTask task) {
        return new DocumentTaskResponse(
                task.getId(),
                task.getDocumentId(),
                task.getDocumentTitle(),
                task.getTaskType(),
                task.getStatus(),
                task.getProcessingStage(),
                task.getSummaryMode(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }

    private String buildErrorMessage(Exception exception) {
        Throwable current = exception;
        String message = exception.getMessage();
        while ((message == null || message.isBlank()) && current.getCause() != null) {
            current = current.getCause();
            message = current.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replaceAll("\\s+", " ").trim();
        if (message.length() > 240) {
            message = message.substring(0, 240) + "...";
        }
        return message;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "unknown";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "unknown" : filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private record ChunkPayload(int index, String type, String content) {
    }

    private record SummaryResult(String content, String type) {
    }
}

