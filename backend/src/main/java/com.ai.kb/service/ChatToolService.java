package com.ai.kb.service;

import com.ai.kb.dto.CategoryRequest;
import com.ai.kb.dto.CategoryResponse;
import com.ai.kb.dto.DocumentListResponse;
import com.ai.kb.dto.DocumentResponse;
import com.ai.kb.dto.KnowledgeBaseRequest;
import com.ai.kb.dto.KnowledgeBaseResponse;
import com.ai.kb.dto.ToolCallResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatToolService {

    private static final String KB = "\u77e5\u8bc6\u5e93";
    private static final String CATEGORY = "\u5206\u7c7b";
    private static final String DOCUMENT = "\u6587\u6863";
    private static final String FILE = "\u6587\u4ef6";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_BLOCKED = "blocked";
    private static final String TOOL_LIST_KNOWLEDGE_BASES = "list_knowledge_bases";
    private static final String TOOL_CREATE_KNOWLEDGE_BASE = "create_knowledge_base";
    private static final String TOOL_RENAME_KNOWLEDGE_BASE = "rename_knowledge_base";
    private static final String TOOL_DELETE_KNOWLEDGE_BASE = "delete_knowledge_base";
    private static final String TOOL_KNOWLEDGE_BASE_STATS = "knowledge_base_stats";
    private static final String TOOL_LIST_DOCUMENTS = "list_documents";
    private static final String TOOL_DOCUMENT_EXISTS = "document_exists";
    private static final String TOOL_MOVE_DOCUMENTS = "move_documents";
    private static final String TOOL_DELETE_DOCUMENTS = "delete_documents";
    private static final String TOOL_LIST_CATEGORIES = "list_categories";
    private static final String TOOL_CREATE_CATEGORY = "create_category";
    private static final String TOOL_RENAME_CATEGORY = "rename_category";
    private static final String TOOL_DELETE_CATEGORY = "delete_category";

    private static final Pattern QUOTED_NAME_PATTERN = Pattern.compile("[\"'\\u201c\\u201d\\u2018\\u2019\\u300a\\u300b\\u300c\\u300d\\u300e\\u300f](.+?)[\"'\\u201c\\u201d\\u2018\\u2019\\u300a\\u300b\\u300c\\u300d\\u300e\\u300f]");
    private static final Pattern RENAME_PATTERN = Pattern.compile("(?:(.+?)(?:\\s*(?:" + KB + "|" + CATEGORY + "))?)?\\s*(?:\\u6539\\u540d|\\u91cd\\u547d\\u540d|\\u66f4\\u540d)(?:\\u4e3a)?\\s*(.+)");
    private static final Pattern DELETE_PATTERN = Pattern.compile("(?:\\u5220\\u9664|\\u79fb\\u9664|\\u5220\\u6389)\\s*(.+)");
    private static final Pattern EXISTS_PATTERN = Pattern.compile("(?:\\u662f\\u5426\\u5b58\\u5728|\\u6709\\u6ca1\\u6709|\\u662f\\u4e0d\\u662f\\u5df2\\u7ecf\\u6709)\\s*(.+)");
    private static final Pattern MOVE_TARGET_PATTERN = Pattern.compile("(?:\\u79fb\\u52a8|\\u79fb\\u5230|\\u79fb\\u5165|\\u8f6c\\u79fb|\\u8f6c\\u5230|\\u5f52\\u7c7b\\u5230|\\u653e\\u5230|\\u653e\\u8fdb|\\u632a\\u5230|\\u79fb\\u81f3)\\s*(.+?)(?:\\u5206\\u7c7b)?$");
    private static final List<String> DELETE_CONFIRMATIONS = List.of(
            "\u786e\u8ba4\u5220\u9664",
            "\u786e\u5b9a\u5220\u9664",
            "\u771f\u7684\u5220\u9664",
            "\u7acb\u5373\u5220\u9664",
            "\u786e\u8ba4\u79fb\u9664",
            "\u786e\u5b9a\u79fb\u9664",
            "\u786e\u8ba4\u5220\u6389",
            "\u786e\u5b9a\u5220\u6389"
    );
    private static final Map<String, ToolDefinition> TOOL_DEFINITIONS = Map.ofEntries(
            Map.entry(TOOL_LIST_KNOWLEDGE_BASES, new ToolDefinition("\u5217\u51fa\u77e5\u8bc6\u5e93", new ToolPolicy(false, false))),
            Map.entry(TOOL_CREATE_KNOWLEDGE_BASE, new ToolDefinition("\u521b\u5efa\u77e5\u8bc6\u5e93", new ToolPolicy(false, false))),
            Map.entry(TOOL_RENAME_KNOWLEDGE_BASE, new ToolDefinition("\u91cd\u547d\u540d\u77e5\u8bc6\u5e93", new ToolPolicy(false, false))),
            Map.entry(TOOL_DELETE_KNOWLEDGE_BASE, new ToolDefinition("\u5220\u9664\u77e5\u8bc6\u5e93", new ToolPolicy(true, true))),
            Map.entry(TOOL_KNOWLEDGE_BASE_STATS, new ToolDefinition("\u67e5\u770b\u77e5\u8bc6\u5e93\u7edf\u8ba1", new ToolPolicy(false, false))),
            Map.entry(TOOL_LIST_DOCUMENTS, new ToolDefinition("\u5217\u51fa\u6587\u6863", new ToolPolicy(false, false))),
            Map.entry(TOOL_DOCUMENT_EXISTS, new ToolDefinition("\u68c0\u67e5\u6587\u6863\u662f\u5426\u5b58\u5728", new ToolPolicy(false, false))),
            Map.entry(TOOL_MOVE_DOCUMENTS, new ToolDefinition("\u79fb\u52a8\u6587\u6863", new ToolPolicy(false, false))),
            Map.entry(TOOL_DELETE_DOCUMENTS, new ToolDefinition("\u5220\u9664\u6587\u6863", new ToolPolicy(true, true))),
            Map.entry(TOOL_LIST_CATEGORIES, new ToolDefinition("\u5217\u51fa\u5206\u7c7b", new ToolPolicy(false, false))),
            Map.entry(TOOL_CREATE_CATEGORY, new ToolDefinition("\u521b\u5efa\u5206\u7c7b", new ToolPolicy(false, false))),
            Map.entry(TOOL_RENAME_CATEGORY, new ToolDefinition("\u91cd\u547d\u540d\u5206\u7c7b", new ToolPolicy(false, false))),
            Map.entry(TOOL_DELETE_CATEGORY, new ToolDefinition("\u5220\u9664\u5206\u7c7b", new ToolPolicy(true, true)))
    );

    private final KnowledgeBaseService knowledgeBaseService;
    private final CategoryService categoryService;
    private final DocumentService documentService;

    public ToolExecutionResult execute(String message, String userId, String knowledgeBaseId) {
        String normalized = normalizeMessage(message);
        List<ToolCallResponse> toolCalls = new ArrayList<>();
        List<String> toolFacts = new ArrayList<>();

        List<KnowledgeBaseResponse> ownedKnowledgeBases = knowledgeBaseService.getListByUser(userId);
        KnowledgeBaseResponse currentKnowledgeBase = resolveCurrentKnowledgeBase(ownedKnowledgeBases, knowledgeBaseId);

        handleKnowledgeBaseTools(message, normalized, userId, currentKnowledgeBase, ownedKnowledgeBases, toolCalls, toolFacts);
        handleDocumentTools(message, normalized, userId, currentKnowledgeBase, toolCalls, toolFacts);
        handleCategoryTools(message, normalized, currentKnowledgeBase, toolCalls, toolFacts);

        return new ToolExecutionResult(toolCalls, String.join("\n\n", toolFacts));
    }

    private void handleKnowledgeBaseTools(
            String message,
            String normalized,
            String userId,
            KnowledgeBaseResponse currentKnowledgeBase,
            List<KnowledgeBaseResponse> ownedKnowledgeBases,
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts
    ) {
        if (wantsKnowledgeBaseList(normalized)) {
            addSuccessToolCall(
                    toolCalls,
                    toolFacts,
                    TOOL_LIST_KNOWLEDGE_BASES,
                    ownedKnowledgeBases.isEmpty()
                            ? "\u5f53\u524d\u8fd8\u6ca1\u6709\u77e5\u8bc6\u5e93\u3002"
                            : "\u5df2\u627e\u5230 " + ownedKnowledgeBases.size() + " \u4e2a\u77e5\u8bc6\u5e93\u3002",
                    formatKnowledgeBases(ownedKnowledgeBases)
            );
        }

        if (wantsKnowledgeBaseCreate(normalized)) {
            String name = extractSingleName(message, "\u65b0\u77e5\u8bc6\u5e93");
            try {
                KnowledgeBaseResponse created = knowledgeBaseService.create(userId, new KnowledgeBaseRequest(name, ""));
                addSuccessToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_CREATE_KNOWLEDGE_BASE,
                        "\u5df2\u521b\u5efa\u77e5\u8bc6\u5e93\u300a" + created.getName() + "\u300b\u3002",
                        "ID: " + created.getId() + "\n\u540d\u79f0: " + created.getName()
                );
            } catch (Exception e) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_CREATE_KNOWLEDGE_BASE,
                        "\u521b\u5efa\u77e5\u8bc6\u5e93\u5931\u8d25\u3002",
                        safeMessage(e)
                );
            }
        }

        if (wantsKnowledgeBaseRename(normalized)) {
            RenameTarget renameTarget = extractRenameTarget(message);
            NamedMatch<KnowledgeBaseResponse> targetMatch = resolveKnowledgeBaseMatch(
                    ownedKnowledgeBases,
                    renameTarget.sourceName(),
                    currentKnowledgeBase
            );
            if (targetMatch.ambiguous()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_RENAME_KNOWLEDGE_BASE,
                        "\u8981\u4fee\u6539\u7684\u77e5\u8bc6\u5e93\u4e0d\u591f\u660e\u786e\u3002",
                        buildAmbiguousDetail("\u77e5\u8bc6\u5e93", renameTarget.sourceName(), targetMatch.candidates())
                );
            } else if (targetMatch.match() == null) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_RENAME_KNOWLEDGE_BASE,
                        "\u627e\u4e0d\u5230\u8981\u4fee\u6539\u7684\u77e5\u8bc6\u5e93\u3002",
                        "\u8bf7\u6307\u5b9a\u77e5\u8bc6\u5e93\u540d\u79f0\uff0c\u6216\u5148\u8fdb\u5165\u76ee\u6807\u77e5\u8bc6\u5e93\u518d\u6267\u884c\u91cd\u547d\u540d\u3002"
                );
            } else {
                try {
                    KnowledgeBaseResponse renamed = knowledgeBaseService.rename(
                            targetMatch.match().getId(),
                            userId,
                            renameTarget.targetName()
                    );
                    addSuccessToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_RENAME_KNOWLEDGE_BASE,
                            "\u5df2\u5c06\u77e5\u8bc6\u5e93\u91cd\u547d\u540d\u4e3a\u300a" + renamed.getName() + "\u300b\u3002",
                            "ID: " + renamed.getId() + "\n\u65b0\u540d\u79f0: " + renamed.getName()
                    );
                } catch (Exception e) {
                    addFailedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_RENAME_KNOWLEDGE_BASE,
                            "\u91cd\u547d\u540d\u77e5\u8bc6\u5e93\u5931\u8d25\u3002",
                            safeMessage(e)
                    );
                }
            }
        }

        if (wantsKnowledgeBaseDelete(normalized)) {
            String targetName = extractDeleteName(message);
            NamedMatch<KnowledgeBaseResponse> targetMatch = resolveKnowledgeBaseMatch(
                    ownedKnowledgeBases,
                    targetName,
                    currentKnowledgeBase
            );
            if (requiresExplicitConfirmation(TOOL_DELETE_KNOWLEDGE_BASE) && !hasDeleteConfirmation(normalized)) {
                addBlockedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_KNOWLEDGE_BASE,
                        "\u5220\u9664\u77e5\u8bc6\u5e93\u9700\u8981\u660e\u786e\u786e\u8ba4\u3002",
                        buildDeleteConfirmationDetail("\u77e5\u8bc6\u5e93", targetName, targetMatch.candidates())
                );
            } else if (targetMatch.ambiguous()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_KNOWLEDGE_BASE,
                        "\u8981\u5220\u9664\u7684\u77e5\u8bc6\u5e93\u4e0d\u591f\u660e\u786e\u3002",
                        buildAmbiguousDetail("\u77e5\u8bc6\u5e93", targetName, targetMatch.candidates())
                );
            } else if (targetMatch.match() == null) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_KNOWLEDGE_BASE,
                        "\u627e\u4e0d\u5230\u8981\u5220\u9664\u7684\u77e5\u8bc6\u5e93\u3002",
                        "\u8bf7\u6307\u5b9a\u77e5\u8bc6\u5e93\u540d\u79f0\uff0c\u6216\u5148\u9009\u4e2d\u76ee\u6807\u77e5\u8bc6\u5e93\u3002"
                );
            } else {
                try {
                    knowledgeBaseService.delete(targetMatch.match().getId(), userId);
                    addSuccessToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_KNOWLEDGE_BASE,
                            "\u5df2\u5220\u9664\u77e5\u8bc6\u5e93\u300a" + targetMatch.match().getName() + "\u300b\u3002",
                            "ID: " + targetMatch.match().getId()
                    );
                } catch (Exception e) {
                    addFailedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_KNOWLEDGE_BASE,
                            "\u5220\u9664\u77e5\u8bc6\u5e93\u5931\u8d25\u3002",
                            safeMessage(e)
                    );
                }
            }
        }

        if (currentKnowledgeBase != null && wantsKnowledgeBaseStats(normalized)) {
            List<CategoryResponse> categories = categoryService.getListByKnowledgeBase(currentKnowledgeBase.getId());
            addSuccessToolCall(
                    toolCalls,
                    toolFacts,
                    TOOL_KNOWLEDGE_BASE_STATS,
                    "\u5df2\u8fd4\u56de\u5f53\u524d\u77e5\u8bc6\u5e93\u300a" + currentKnowledgeBase.getName() + "\u300b\u7684\u7edf\u8ba1\u4fe1\u606f\u3002",
                    "\u77e5\u8bc6\u5e93: " + currentKnowledgeBase.getName()
                            + "\n\u6587\u6863\u6570: " + safeCount(currentKnowledgeBase.getDocumentCount())
                            + "\n\u5206\u7c7b\u6570: " + categories.size()
            );
        }
    }

    private void handleDocumentTools(
            String message,
            String normalized,
            String userId,
            KnowledgeBaseResponse currentKnowledgeBase,
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts
    ) {
        if (currentKnowledgeBase != null && wantsDocumentList(normalized)) {
            DocumentListResponse documents = documentService.getList(currentKnowledgeBase.getId(), null, 1, 20);
            addSuccessToolCall(
                    toolCalls,
                    toolFacts,
                    TOOL_LIST_DOCUMENTS,
                    documents.list().isEmpty()
                            ? "\u5f53\u524d\u77e5\u8bc6\u5e93\u4e0b\u8fd8\u6ca1\u6709\u6587\u6863\u3002"
                            : "\u5df2\u627e\u5230 " + documents.list().size() + " \u7bc7\u6587\u6863\u3002",
                    formatDocuments(documents.list())
            );
        }

        if (currentKnowledgeBase != null && wantsDocumentExists(normalized)) {
            String documentName = extractDocumentName(message);
            if (documentName == null || documentName.isBlank()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DOCUMENT_EXISTS,
                        "\u65e0\u6cd5\u786e\u5b9a\u8981\u68c0\u67e5\u7684\u6587\u6863\u540d\u79f0\u3002",
                        "\u8bf7\u76f4\u63a5\u7ed9\u51fa\u6587\u6863\u6807\u9898\uff0c\u6700\u597d\u4f7f\u7528\u5f15\u53f7\u5305\u8d77\u6765\u3002"
                );
            } else {
                boolean exists = documentService.existsInKnowledgeBase(currentKnowledgeBase.getId(), documentName);
                addSuccessToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DOCUMENT_EXISTS,
                        exists
                                ? "\u5728\u5f53\u524d\u77e5\u8bc6\u5e93\u4e2d\u627e\u5230\u4e86\u540c\u540d\u6587\u6863\u3002"
                                : "\u5f53\u524d\u77e5\u8bc6\u5e93\u4e2d\u6ca1\u6709\u627e\u5230\u540c\u540d\u6587\u6863\u3002",
                        "\u77e5\u8bc6\u5e93: " + currentKnowledgeBase.getName()
                                + "\n\u6587\u6863\u6807\u9898: " + documentName
                                + "\n\u7ed3\u679c: " + (exists ? "\u5df2\u5b58\u5728" : "\u4e0d\u5b58\u5728")
                );
            }
        }

        if (currentKnowledgeBase != null && wantsDocumentMove(normalized)) {
            MoveDocumentTarget moveTarget = extractMoveDocumentTarget(message);
            if (moveTarget.documentNames().isEmpty()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_MOVE_DOCUMENTS,
                        "\u65e0\u6cd5\u786e\u5b9a\u8981\u79fb\u52a8\u7684\u6587\u6863\u3002",
                        "\u8bf7\u76f4\u63a5\u7ed9\u51fa\u6587\u6863\u540d\u79f0\uff0c\u6700\u597d\u4f7f\u7528\u5f15\u53f7\u6216\u4e66\u540d\u53f7\u5305\u8d77\u6765\u3002"
                );
            } else {
                String targetCategoryId = null;
                String targetCategoryLabel = "\u672a\u5206\u7c7b";
                boolean canMove = true;
                if (!isUncategorizedName(moveTarget.categoryName())) {
                    NamedMatch<CategoryResponse> targetCategoryMatch = resolveCategoryMatch(
                            currentKnowledgeBase.getId(),
                            moveTarget.categoryName()
                    );
                    if (targetCategoryMatch.ambiguous()) {
                        canMove = false;
                        addFailedToolCall(
                                toolCalls,
                                toolFacts,
                                TOOL_MOVE_DOCUMENTS,
                                "\u76ee\u6807\u5206\u7c7b\u4e0d\u591f\u660e\u786e\u3002",
                                buildAmbiguousDetail("\u5206\u7c7b", moveTarget.categoryName(), targetCategoryMatch.candidates())
                        );
                    } else if (targetCategoryMatch.match() == null) {
                        canMove = false;
                        addFailedToolCall(
                                toolCalls,
                                toolFacts,
                                TOOL_MOVE_DOCUMENTS,
                                "\u627e\u4e0d\u5230\u76ee\u6807\u5206\u7c7b\u3002",
                                "\u8bf7\u76f4\u63a5\u7ed9\u51fa\u5206\u7c7b\u540d\u79f0\uff0c\u6216\u4f7f\u7528\u201c\u672a\u5206\u7c7b\u201d\u8868\u793a\u79fb\u51fa\u5206\u7c7b\u3002"
                        );
                    } else {
                        targetCategoryId = targetCategoryMatch.match().id();
                        targetCategoryLabel = targetCategoryMatch.match().name();
                    }
                }

                if (canMove) {
                    BatchDocumentResolution resolution = resolveDocuments(currentKnowledgeBase.getId(), moveTarget.documentNames());

                    if (!resolution.ambiguousDetails().isEmpty()) {
                        addFailedToolCall(
                                toolCalls,
                                toolFacts,
                                TOOL_MOVE_DOCUMENTS,
                                "\u8981\u79fb\u52a8\u7684\u6587\u6863\u4e0d\u591f\u660e\u786e\u3002",
                                buildBatchAmbiguousDetail(resolution.ambiguousDetails())
                        );
                    } else if (resolution.matchedDocuments().isEmpty()) {
                        addFailedToolCall(
                                toolCalls,
                                toolFacts,
                                TOOL_MOVE_DOCUMENTS,
                                "\u627e\u4e0d\u5230\u8981\u79fb\u52a8\u7684\u6587\u6863\u3002",
                                "\u8bf7\u786e\u8ba4\u6587\u6863\u540d\u79f0\u662f\u5426\u6b63\u786e\uff0c\u6216\u5148\u5217\u51fa\u5f53\u524d\u77e5\u8bc6\u5e93\u4e0b\u7684\u6587\u6863\u3002"
                        );
                    } else {
                        try {
                            List<DocumentResponse> movedDocuments = documentService.setCategoryBatch(
                                    resolution.matchedDocuments().stream().map(DocumentResponse::id).toList(),
                                    targetCategoryId,
                                    userId
                            );
                            addSuccessToolCall(
                                    toolCalls,
                                    toolFacts,
                                    TOOL_MOVE_DOCUMENTS,
                                    "\u5df2\u79fb\u52a8 " + movedDocuments.size() + " \u7bc7\u6587\u6863\u5230\u300a" + targetCategoryLabel + "\u300b\u3002"
                                            + buildMissingSuffix(resolution.missingDocuments()),
                                    "\u76ee\u6807\u5206\u7c7b: " + targetCategoryLabel
                                            + "\n\u5df2\u79fb\u52a8:\n" + formatDocumentTitles(movedDocuments)
                                            + buildMissingDetail(resolution.missingDocuments())
                            );
                        } catch (Exception e) {
                            addFailedToolCall(
                                    toolCalls,
                                    toolFacts,
                                    TOOL_MOVE_DOCUMENTS,
                                    "\u79fb\u52a8\u6587\u6863\u5931\u8d25\u3002",
                                    safeMessage(e)
                            );
                        }
                    }
                }
            }
        }

        if (currentKnowledgeBase != null && wantsDocumentDelete(normalized)) {
            List<String> documentNames = extractDocumentNames(message);
            if (documentNames.isEmpty()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_DOCUMENTS,
                        "\u65e0\u6cd5\u786e\u5b9a\u8981\u5220\u9664\u7684\u6587\u6863\u3002",
                        "\u8bf7\u76f4\u63a5\u7ed9\u51fa\u6587\u6863\u540d\u79f0\uff0c\u6700\u597d\u4f7f\u7528\u5f15\u53f7\u6216\u4e66\u540d\u53f7\u5305\u8d77\u6765\u3002"
                );
            } else {
                BatchDocumentResolution resolution = resolveDocuments(currentKnowledgeBase.getId(), documentNames);
                if (requiresExplicitConfirmation(TOOL_DELETE_DOCUMENTS) && !hasDeleteConfirmation(normalized)) {
                    addBlockedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_DOCUMENTS,
                            "\u5220\u9664\u6587\u6863\u9700\u8981\u660e\u786e\u786e\u8ba4\u3002",
                            buildDeleteConfirmationDetail("\u6587\u6863", String.join("\u3001", documentNames), resolution.allCandidates())
                    );
                } else if (!resolution.ambiguousDetails().isEmpty()) {
                    addFailedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_DOCUMENTS,
                            "\u8981\u5220\u9664\u7684\u6587\u6863\u4e0d\u591f\u660e\u786e\u3002",
                            buildBatchAmbiguousDetail(resolution.ambiguousDetails())
                    );
                } else if (resolution.matchedDocuments().isEmpty()) {
                    addFailedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_DOCUMENTS,
                            "\u627e\u4e0d\u5230\u8981\u5220\u9664\u7684\u6587\u6863\u3002",
                            "\u8bf7\u786e\u8ba4\u6587\u6863\u540d\u79f0\u662f\u5426\u6b63\u786e\uff0c\u6216\u5148\u5217\u51fa\u5f53\u524d\u77e5\u8bc6\u5e93\u4e0b\u7684\u6587\u6863\u3002"
                    );
                } else {
                    try {
                        List<DocumentResponse> deletedDocuments = documentService.deleteBatch(
                                resolution.matchedDocuments().stream().map(DocumentResponse::id).toList(),
                                userId
                        );
                        addSuccessToolCall(
                                toolCalls,
                                toolFacts,
                                TOOL_DELETE_DOCUMENTS,
                                "\u5df2\u5220\u9664 " + deletedDocuments.size() + " \u7bc7\u6587\u6863\u3002" + buildMissingSuffix(resolution.missingDocuments()),
                                "\u5df2\u5220\u9664:\n" + formatDocumentTitles(deletedDocuments) + buildMissingDetail(resolution.missingDocuments())
                        );
                    } catch (Exception e) {
                        addFailedToolCall(
                                toolCalls,
                                toolFacts,
                                TOOL_DELETE_DOCUMENTS,
                                "\u5220\u9664\u6587\u6863\u5931\u8d25\u3002",
                                safeMessage(e)
                        );
                    }
                }
            }
        }
    }

    private void handleCategoryTools(
            String message,
            String normalized,
            KnowledgeBaseResponse currentKnowledgeBase,
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts
    ) {
        if (currentKnowledgeBase != null && wantsCategoryList(normalized)) {
            List<CategoryResponse> categories = categoryService.getListByKnowledgeBase(currentKnowledgeBase.getId());
            addSuccessToolCall(
                    toolCalls,
                    toolFacts,
                    TOOL_LIST_CATEGORIES,
                    categories.isEmpty()
                            ? "\u5f53\u524d\u77e5\u8bc6\u5e93\u4e0b\u8fd8\u6ca1\u6709\u5206\u7c7b\u3002"
                            : "\u5df2\u627e\u5230 " + categories.size() + " \u4e2a\u5206\u7c7b\u3002",
                    formatCategories(categories)
            );
        }

        if (currentKnowledgeBase != null && wantsCategoryCreate(normalized)) {
            String categoryName = extractSingleName(message, "\u672a\u547d\u540d\u5206\u7c7b");
            try {
                CategoryResponse created = categoryService.create(currentKnowledgeBase.getId(), new CategoryRequest(categoryName, null));
                addSuccessToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_CREATE_CATEGORY,
                        "\u5df2\u521b\u5efa\u5206\u7c7b\u300a" + created.name() + "\u300b\u3002",
                        "ID: " + created.id() + "\n\u540d\u79f0: " + created.name()
                );
            } catch (Exception e) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_CREATE_CATEGORY,
                        "\u521b\u5efa\u5206\u7c7b\u5931\u8d25\u3002",
                        safeMessage(e)
                );
            }
        }

        if (currentKnowledgeBase != null && wantsCategoryRename(normalized)) {
            RenameTarget renameTarget = extractRenameTarget(message);
            NamedMatch<CategoryResponse> targetMatch = resolveCategoryMatch(
                    currentKnowledgeBase.getId(),
                    renameTarget.sourceName()
            );
            if (targetMatch.ambiguous()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_RENAME_CATEGORY,
                        "\u8981\u4fee\u6539\u7684\u5206\u7c7b\u4e0d\u591f\u660e\u786e\u3002",
                        buildAmbiguousDetail("\u5206\u7c7b", renameTarget.sourceName(), targetMatch.candidates())
                );
            } else if (targetMatch.match() == null) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_RENAME_CATEGORY,
                        "\u627e\u4e0d\u5230\u8981\u4fee\u6539\u7684\u5206\u7c7b\u3002",
                        "\u8bf7\u76f4\u63a5\u7ed9\u51fa\u5206\u7c7b\u540d\u79f0\uff0c\u6700\u597d\u4f7f\u7528\u5f15\u53f7\u5305\u8d77\u6765\u3002"
                );
            } else {
                try {
                    CategoryResponse renamed = categoryService.update(targetMatch.match().id(), renameTarget.targetName());
                    addSuccessToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_RENAME_CATEGORY,
                            "\u5df2\u5c06\u5206\u7c7b\u91cd\u547d\u540d\u4e3a\u300a" + renamed.name() + "\u300b\u3002",
                            "ID: " + renamed.id() + "\n\u65b0\u540d\u79f0: " + renamed.name()
                    );
                } catch (Exception e) {
                    addFailedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_RENAME_CATEGORY,
                            "\u91cd\u547d\u540d\u5206\u7c7b\u5931\u8d25\u3002",
                            safeMessage(e)
                    );
                }
            }
        }

        if (currentKnowledgeBase != null && wantsCategoryDelete(normalized)) {
            String categoryName = extractDeleteName(message);
            NamedMatch<CategoryResponse> targetMatch = resolveCategoryMatch(currentKnowledgeBase.getId(), categoryName);
            if (requiresExplicitConfirmation(TOOL_DELETE_CATEGORY) && !hasDeleteConfirmation(normalized)) {
                addBlockedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_CATEGORY,
                        "\u5220\u9664\u5206\u7c7b\u9700\u8981\u660e\u786e\u786e\u8ba4\u3002",
                        buildDeleteConfirmationDetail("\u5206\u7c7b", categoryName, targetMatch.candidates())
                );
            } else if (targetMatch.ambiguous()) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_CATEGORY,
                        "\u8981\u5220\u9664\u7684\u5206\u7c7b\u4e0d\u591f\u660e\u786e\u3002",
                        buildAmbiguousDetail("\u5206\u7c7b", categoryName, targetMatch.candidates())
                );
            } else if (targetMatch.match() == null) {
                addFailedToolCall(
                        toolCalls,
                        toolFacts,
                        TOOL_DELETE_CATEGORY,
                        "\u627e\u4e0d\u5230\u8981\u5220\u9664\u7684\u5206\u7c7b\u3002",
                        "\u8bf7\u76f4\u63a5\u7ed9\u51fa\u5206\u7c7b\u540d\u79f0\uff0c\u6700\u597d\u4f7f\u7528\u5f15\u53f7\u5305\u8d77\u6765\u3002"
                );
            } else {
                try {
                    categoryService.delete(targetMatch.match().id());
                    addSuccessToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_CATEGORY,
                            "\u5df2\u5220\u9664\u5206\u7c7b\u300a" + targetMatch.match().name() + "\u300b\u3002",
                            "ID: " + targetMatch.match().id()
                    );
                } catch (Exception e) {
                    addFailedToolCall(
                            toolCalls,
                            toolFacts,
                            TOOL_DELETE_CATEGORY,
                            "\u5220\u9664\u5206\u7c7b\u5931\u8d25\u3002",
                            safeMessage(e)
                        );
                }
            }
        }
    }

    private void addSuccessToolCall(
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts,
            String name,
            String summary,
            String detail
    ) {
        addSuccessToolCall(toolCalls, toolFacts, name, resolveToolTitle(name), summary, detail);
    }

    private void addSuccessToolCall(
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts,
            String name,
            String title,
            String summary,
            String detail
    ) {
        toolCalls.add(new ToolCallResponse(name, title, STATUS_SUCCESS, summary, detail));
        toolFacts.add(title + ":\n" + detail);
    }

    private void addBlockedToolCall(
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts,
            String name,
            String summary,
            String detail
    ) {
        addBlockedToolCall(toolCalls, toolFacts, name, resolveToolTitle(name), summary, detail);
    }

    private void addBlockedToolCall(
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts,
            String name,
            String title,
            String summary,
            String detail
    ) {
        toolCalls.add(new ToolCallResponse(name, title, STATUS_BLOCKED, summary, detail));
        toolFacts.add(title + "\u5f85\u786e\u8ba4: " + detail);
    }

    private void addFailedToolCall(
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts,
            String name,
            String summary,
            String detail
    ) {
        addFailedToolCall(toolCalls, toolFacts, name, resolveToolTitle(name), summary, detail);
    }

    private void addFailedToolCall(
            List<ToolCallResponse> toolCalls,
            List<String> toolFacts,
            String name,
            String title,
            String summary,
            String detail
    ) {
        toolCalls.add(new ToolCallResponse(name, title, STATUS_FAILED, summary, detail));
        toolFacts.add(title + "\u5931\u8d25: " + detail);
    }

    private boolean wantsKnowledgeBaseList(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, "\u6709\u54ea\u4e9b", "\u5217\u51fa", "\u5217\u8868", "\u67e5\u770b", "\u770b\u770b", "\u6211\u7684");
    }

    private boolean wantsKnowledgeBaseCreate(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, "\u521b\u5efa", "\u65b0\u5efa", "\u65b0\u589e", "\u5efa\u7acb");
    }

    private boolean wantsKnowledgeBaseRename(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, "\u6539\u540d", "\u91cd\u547d\u540d", "\u66f4\u540d");
    }

    private boolean wantsKnowledgeBaseDelete(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, "\u5220\u9664", "\u79fb\u9664", "\u5220\u6389");
    }

    private boolean wantsKnowledgeBaseStats(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, "\u7edf\u8ba1", "\u6982\u51b5", "\u60c5\u51b5", "\u4fe1\u606f");
    }

    private boolean wantsDocumentList(String normalized) {
        return containsAny(normalized, DOCUMENT, FILE) && containsAny(normalized, "\u6709\u54ea\u4e9b", "\u5217\u51fa", "\u5217\u8868", "\u67e5\u770b", "\u770b\u770b");
    }

    private boolean wantsDocumentExists(String normalized) {
        return containsAny(normalized, DOCUMENT, FILE) && containsAny(normalized, "\u662f\u5426\u5b58\u5728", "\u6709\u6ca1\u6709", "\u662f\u4e0d\u662f\u5df2\u7ecf\u6709");
    }

    private boolean wantsDocumentMove(String normalized) {
        return containsAny(normalized, DOCUMENT, FILE)
                && containsAny(normalized, "\u79fb\u52a8", "\u79fb\u5230", "\u79fb\u5165", "\u8f6c\u79fb", "\u8f6c\u5230", "\u5f52\u7c7b", "\u653e\u5230", "\u653e\u8fdb", "\u632a\u5230", "\u79fb\u81f3");
    }

    private boolean wantsDocumentDelete(String normalized) {
        return containsAny(normalized, DOCUMENT, FILE)
                && containsAny(normalized, "\u5220\u9664", "\u79fb\u9664", "\u5220\u6389");
    }

    private boolean wantsCategoryList(String normalized) {
        return normalized.contains(CATEGORY) && containsAny(normalized, "\u6709\u54ea\u4e9b", "\u5217\u51fa", "\u5217\u8868", "\u67e5\u770b", "\u770b\u770b");
    }

    private boolean wantsCategoryCreate(String normalized) {
        return normalized.contains(CATEGORY) && containsAny(normalized, "\u521b\u5efa", "\u65b0\u5efa", "\u65b0\u589e", "\u5efa\u7acb");
    }

    private boolean wantsCategoryRename(String normalized) {
        return normalized.contains(CATEGORY) && containsAny(normalized, "\u6539\u540d", "\u91cd\u547d\u540d", "\u66f4\u540d");
    }

    private boolean wantsCategoryDelete(String normalized) {
        return normalized.contains(CATEGORY) && containsAny(normalized, "\u5220\u9664", "\u79fb\u9664", "\u5220\u6389");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasDeleteConfirmation(String normalizedMessage) {
        return DELETE_CONFIRMATIONS.stream().anyMatch(normalizedMessage::contains);
    }

    private boolean requiresExplicitConfirmation(String toolName) {
        ToolDefinition definition = TOOL_DEFINITIONS.get(toolName);
        return definition != null && definition.policy().requiresConfirmation();
    }

    private String resolveToolTitle(String toolName) {
        ToolDefinition definition = TOOL_DEFINITIONS.get(toolName);
        return definition == null ? toolName : definition.title();
    }

    private KnowledgeBaseResponse resolveCurrentKnowledgeBase(List<KnowledgeBaseResponse> knowledgeBases, String knowledgeBaseId) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            return null;
        }
        return knowledgeBases.stream()
                .filter(item -> Objects.equals(item.getId(), knowledgeBaseId))
                .findFirst()
                .orElse(null);
    }

    private NamedMatch<KnowledgeBaseResponse> resolveKnowledgeBaseMatch(
            List<KnowledgeBaseResponse> knowledgeBases,
            String name,
            KnowledgeBaseResponse fallback
    ) {
        if ((name == null || name.isBlank()) && fallback != null) {
            return new NamedMatch<>(fallback, List.of(fallback.getName()));
        }
        return resolveSingleMatch(
                knowledgeBases,
                name,
                KnowledgeBaseResponse::getName
        );
    }

    private KnowledgeBaseResponse resolveKnowledgeBaseByName(
            List<KnowledgeBaseResponse> knowledgeBases,
            String name,
            KnowledgeBaseResponse fallback
    ) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String normalized = normalizeEntityName(name);
        return knowledgeBases.stream()
                .filter(item -> normalizeEntityName(item.getName()).equals(normalized))
                .findFirst()
                .orElseGet(() -> knowledgeBases.stream()
                        .filter(item -> normalizeEntityName(item.getName()).contains(normalized))
                        .findFirst()
                        .orElse(null));
    }

    private NamedMatch<CategoryResponse> resolveCategoryMatch(String knowledgeBaseId, String name) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            return new NamedMatch<>(null, List.of());
        }
        return resolveSingleMatch(
                categoryService.getListByKnowledgeBase(knowledgeBaseId),
                name,
                CategoryResponse::name
        );
    }

    private CategoryResponse resolveCategoryByName(String knowledgeBaseId, String name) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank() || name == null || name.isBlank()) {
            return null;
        }
        String normalized = normalizeEntityName(name);
        List<CategoryResponse> categories = categoryService.getListByKnowledgeBase(knowledgeBaseId);
        return categories.stream()
                .filter(item -> normalizeEntityName(item.name()).equals(normalized))
                .findFirst()
                .orElseGet(() -> categories.stream()
                        .filter(item -> normalizeEntityName(item.name()).contains(normalized))
                        .findFirst()
                        .orElse(null));
    }

    private <T> NamedMatch<T> resolveSingleMatch(List<T> items, String name, java.util.function.Function<T, String> nameExtractor) {
        if (items == null || items.isEmpty() || name == null || name.isBlank()) {
            return new NamedMatch<>(null, List.of());
        }

        String normalized = normalizeEntityName(name);
        List<T> exactMatches = items.stream()
                .filter(item -> normalizeEntityName(nameExtractor.apply(item)).equals(normalized))
                .toList();
        if (exactMatches.size() == 1) {
            return new NamedMatch<>(exactMatches.get(0), List.of(nameExtractor.apply(exactMatches.get(0))));
        }
        if (exactMatches.size() > 1) {
            return new NamedMatch<>(null, exactMatches.stream().map(nameExtractor).toList());
        }

        List<T> partialMatches = items.stream()
                .filter(item -> normalizeEntityName(nameExtractor.apply(item)).contains(normalized))
                .toList();
        if (partialMatches.size() == 1) {
            return new NamedMatch<>(partialMatches.get(0), List.of(nameExtractor.apply(partialMatches.get(0))));
        }
        if (partialMatches.size() > 1) {
            return new NamedMatch<>(null, partialMatches.stream().map(nameExtractor).toList());
        }
        return new NamedMatch<>(null, List.of());
    }

    private BatchDocumentResolution resolveDocuments(String knowledgeBaseId, List<String> requestedNames) {
        List<DocumentResponse> allDocuments = documentService.findAllByKnowledgeBase(knowledgeBaseId);
        Map<String, DocumentResponse> matchedById = new LinkedHashMap<>();
        List<String> missingDocuments = new ArrayList<>();
        List<String> ambiguousDetails = new ArrayList<>();
        Set<String> candidateNames = new LinkedHashSet<>();

        for (String requestedName : requestedNames) {
            String normalizedRequested = normalizeEntityName(requestedName);
            if (normalizedRequested.isBlank()) {
                continue;
            }

            List<DocumentResponse> exactMatches = allDocuments.stream()
                    .filter(document -> normalizeEntityName(document.title()).equals(normalizedRequested))
                    .toList();
            if (exactMatches.size() == 1) {
                DocumentResponse match = exactMatches.get(0);
                matchedById.put(match.id(), match);
                candidateNames.add(match.title());
                continue;
            }
            if (exactMatches.size() > 1) {
                ambiguousDetails.add(buildDocumentAmbiguousDetail(requestedName, exactMatches));
                candidateNames.addAll(exactMatches.stream().map(DocumentResponse::title).toList());
                continue;
            }

            List<DocumentResponse> partialMatches = allDocuments.stream()
                    .filter(document -> normalizeEntityName(document.title()).contains(normalizedRequested))
                    .toList();
            if (partialMatches.size() == 1) {
                DocumentResponse match = partialMatches.get(0);
                matchedById.put(match.id(), match);
                candidateNames.add(match.title());
            } else if (partialMatches.size() > 1) {
                ambiguousDetails.add(buildDocumentAmbiguousDetail(requestedName, partialMatches));
                candidateNames.addAll(partialMatches.stream().map(DocumentResponse::title).toList());
            } else {
                missingDocuments.add(requestedName);
            }
        }

        return new BatchDocumentResolution(
                new ArrayList<>(matchedById.values()),
                missingDocuments,
                ambiguousDetails,
                new ArrayList<>(candidateNames)
        );
    }

    private RenameTarget extractRenameTarget(String message) {
        if (message == null || message.isBlank()) {
            return new RenameTarget(null, "\u672a\u547d\u540d");
        }

        List<String> quotedNames = extractQuotedNames(message);
        if (quotedNames.size() >= 2) {
            return new RenameTarget(quotedNames.get(0), quotedNames.get(1));
        }

        Matcher matcher = RENAME_PATTERN.matcher(message);
        if (matcher.find()) {
            String sourceName = cleanupName(matcher.group(1), null);
            String targetName = cleanupName(matcher.group(2), "\u672a\u547d\u540d");
            return new RenameTarget(sourceName, targetName);
        }

        if (quotedNames.size() == 1) {
            return new RenameTarget(null, cleanupName(quotedNames.get(0), "\u672a\u547d\u540d"));
        }

        return new RenameTarget(null, extractSingleName(message, "\u672a\u547d\u540d"));
    }

    private String extractDeleteName(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        List<String> quotedNames = extractQuotedNames(message);
        if (!quotedNames.isEmpty()) {
            return cleanupName(quotedNames.get(0), null);
        }
        Matcher matcher = DELETE_PATTERN.matcher(message);
        if (matcher.find()) {
            return cleanupName(matcher.group(1), null);
        }
        return null;
    }

    private String extractDocumentName(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        List<String> quotedNames = extractQuotedNames(message);
        if (!quotedNames.isEmpty()) {
            return cleanupName(quotedNames.get(0), null);
        }
        Matcher matcher = EXISTS_PATTERN.matcher(message);
        if (matcher.find()) {
            return cleanupName(matcher.group(1), null);
        }
        return null;
    }

    private MoveDocumentTarget extractMoveDocumentTarget(String message) {
        List<String> quotedNames = extractQuotedNames(message);
        if (quotedNames.size() >= 2) {
            return new MoveDocumentTarget(
                    quotedNames.subList(0, quotedNames.size() - 1),
                    cleanupName(quotedNames.get(quotedNames.size() - 1), null)
            );
        }

        Matcher matcher = MOVE_TARGET_PATTERN.matcher(message == null ? "" : message);
        String categoryName = matcher.find() ? cleanupName(matcher.group(1), null) : null;
        String sourceSegment = message == null ? "" : message;
        if (matcher.find(0)) {
            sourceSegment = message.substring(0, matcher.start());
        }
        return new MoveDocumentTarget(extractNamesFromText(sourceSegment), categoryName);
    }

    private List<String> extractDocumentNames(String message) {
        List<String> quotedNames = extractQuotedNames(message);
        if (!quotedNames.isEmpty()) {
            return quotedNames;
        }
        if (message == null || message.isBlank()) {
            return List.of();
        }
        Matcher matcher = DELETE_PATTERN.matcher(message);
        if (matcher.find()) {
            return extractNamesFromText(matcher.group(1));
        }
        return List.of();
    }

    private String extractSingleName(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        List<String> quotedNames = extractQuotedNames(message);
        if (!quotedNames.isEmpty()) {
            return cleanupName(quotedNames.get(quotedNames.size() - 1), fallback);
        }
        return fallback;
    }

    private List<String> extractQuotedNames(String message) {
        List<String> names = new ArrayList<>();
        Matcher matcher = QUOTED_NAME_PATTERN.matcher(message);
        while (matcher.find()) {
            String name = cleanupName(matcher.group(1), null);
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private String cleanupName(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String cleaned = raw.trim()
                .replace(KB, "")
                .replace(CATEGORY, "")
                .replace(DOCUMENT, "")
                .replace(FILE, "")
                .trim();
        if (cleaned.isBlank()) {
            return fallback;
        }
        return cleaned.length() > 60 ? cleaned.substring(0, 60).trim() : cleaned;
    }

    private List<String> extractNamesFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text
                .replace("\u628a", " ")
                .replace("\u5c06", " ")
                .replace("\u8fd9\u4e9b", " ")
                .replace("\u8fd9\u51e0\u4e2a", " ")
                .replace("\u8fd9\u6279", " ")
                .replace("\u6279\u91cf", " ")
                .replace("\u4e00\u8d77", " ")
                .replace("\u5168\u90e8", " ")
                .replace("\u90fd", " ")
                .replace(DOCUMENT, " ")
                .replace(FILE, " ")
                .replace("\u79fb\u52a8", " ")
                .replace("\u79fb\u5230", " ")
                .replace("\u79fb\u5165", " ")
                .replace("\u8f6c\u79fb", " ")
                .replace("\u8f6c\u5230", " ")
                .replace("\u5220\u9664", " ")
                .replace("\u79fb\u9664", " ")
                .replace("\u5220\u6389", " ")
                .trim();

        List<String> names = new ArrayList<>();
        for (String part : normalized.split("[,，、\\n]")) {
            String cleaned = cleanupName(part.replace("\u548c", " ").replace("\u53ca", " ").trim(), null);
            if (cleaned != null && !cleaned.isBlank()) {
                names.add(cleaned);
            }
        }
        return names;
    }

    private String normalizeEntityName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatKnowledgeBases(List<KnowledgeBaseResponse> list) {
        if (list.isEmpty()) {
            return "\u6682\u65e0\u77e5\u8bc6\u5e93";
        }
        return String.join("\n", list.stream()
                .map(item -> "- " + item.getName() + "\uff08\u6587\u6863 " + safeCount(item.getDocumentCount()) + " \u7bc7\uff09")
                .toList());
    }

    private String formatDocuments(List<DocumentResponse> documents) {
        if (documents.isEmpty()) {
            return "\u6682\u65e0\u6587\u6863";
        }
        return String.join("\n", documents.stream()
                .map(item -> "- " + item.title() + " [" + safeText(item.status()) + "]")
                .toList());
    }

    private String formatDocumentTitles(List<DocumentResponse> documents) {
        if (documents.isEmpty()) {
            return "- \u6682\u65e0";
        }
        return String.join("\n", documents.stream()
                .map(item -> "- " + item.title())
                .toList());
    }

    private String formatCategories(List<CategoryResponse> categories) {
        if (categories.isEmpty()) {
            return "\u6682\u65e0\u5206\u7c7b";
        }
        return String.join("\n", categories.stream()
                .map(item -> "- " + item.name() + "\uff08\u6587\u6863 " + safeCount(item.documentCount()) + " \u7bc7\uff09")
                .toList());
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 240 ? message.substring(0, 240) + "..." : message;
    }

    private List<String> findMissingNames(List<String> requested, List<String> matchedTitles) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        for (String name : requested) {
            String normalizedRequested = normalizeEntityName(name);
            boolean exists = matchedTitles.stream()
                    .map(this::normalizeEntityName)
                    .anyMatch(title -> title.equals(normalizedRequested) || title.contains(normalizedRequested));
            if (!exists) {
                missing.add(name);
            }
        }
        return missing;
    }

    private boolean isUncategorizedName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = normalizeEntityName(name);
        return normalized.contains("\u672a\u5206\u7c7b")
                || normalized.contains("\u65e0\u5206\u7c7b")
                || normalized.contains("\u4e0d\u5206\u7c7b")
                || normalized.contains("\u7a7a\u5206\u7c7b");
    }

    private String buildMissingSuffix(List<String> missingDocuments) {
        if (missingDocuments == null || missingDocuments.isEmpty()) {
            return "";
        }
        return "\u672a\u547d\u4e2d " + missingDocuments.size() + " \u7bc7\u3002";
    }

    private String buildMissingDetail(List<String> missingDocuments) {
        if (missingDocuments == null || missingDocuments.isEmpty()) {
            return "";
        }
        return "\n\u672a\u627e\u5230:\n" + String.join("\n", missingDocuments.stream()
                .map(name -> "- " + name)
                .toList());
    }

    private String buildAmbiguousDetail(String entityLabel, String requestedName, List<String> candidates) {
        StringBuilder detail = new StringBuilder();
        if (requestedName == null || requestedName.isBlank()) {
            detail.append("\u8bf7\u76f4\u63a5\u8bf4\u660e\u8981\u64cd\u4f5c\u7684").append(entityLabel).append("\u540d\u79f0\u3002");
        } else {
            detail.append("\u201c").append(requestedName).append("\u201d\u5339\u914d\u5230\u4e86\u591a\u4e2a").append(entityLabel).append("\uff1a");
        }
        if (candidates != null && !candidates.isEmpty()) {
            detail.append("\n").append(String.join("\n", candidates.stream()
                    .map(name -> "- " + name)
                    .toList()));
        }
        detail.append("\n\u8bf7\u7528\u66f4\u5b8c\u6574\u7684\u540d\u79f0\u91cd\u8bd5\u3002");
        return detail.toString();
    }

    private String buildBatchAmbiguousDetail(List<String> ambiguousDetails) {
        return String.join("\n\n", ambiguousDetails)
                + "\n\n\u8bf7\u628a\u6587\u6863\u540d\u79f0\u8bf4\u5f97\u66f4\u5b8c\u6574\uff0c\u6216\u5148\u5217\u51fa\u6587\u6863\u518d\u64cd\u4f5c\u3002";
    }

    private String buildDocumentAmbiguousDetail(String requestedName, List<DocumentResponse> candidates) {
        return "\u201c" + requestedName + "\u201d\u5339\u914d\u5230\u591a\u7bc7\u6587\u6863\uff1a\n"
                + String.join("\n", candidates.stream()
                .map(document -> "- " + document.title())
                .toList());
    }

    private String buildDeleteConfirmationDetail(String entityLabel, String requestedName, List<String> candidates) {
        StringBuilder detail = new StringBuilder("\u8fd9\u662f\u7834\u574f\u6027\u64cd\u4f5c\uff0c\u8bf7\u5728\u6d88\u606f\u4e2d\u660e\u786e\u8bf4\u201c\u786e\u8ba4\u5220\u9664\u201d\u3002");
        if (requestedName != null && !requestedName.isBlank()) {
            detail.append("\n\u4f8b\u5982\uff1a\u786e\u8ba4\u5220\u9664").append(entityLabel).append("\u300a").append(requestedName).append("\u300b");
        }
        if (candidates != null && !candidates.isEmpty()) {
            detail.append("\n\u5f53\u524d\u53ef\u80fd\u7684\u76ee\u6807\uff1a\n")
                    .append(String.join("\n", candidates.stream()
                            .map(name -> "- " + name)
                            .toList()));
        }
        return detail.toString();
    }

    private record NamedMatch<T>(T match, List<String> candidates) {
        private boolean ambiguous() {
            return match == null && candidates != null && candidates.size() > 1;
        }
    }

    private record BatchDocumentResolution(
            List<DocumentResponse> matchedDocuments,
            List<String> missingDocuments,
            List<String> ambiguousDetails,
            List<String> allCandidates
    ) {
    }

    private record ToolDefinition(String title, ToolPolicy policy) {
    }

    private record ToolPolicy(boolean destructive, boolean requiresConfirmation) {
    }

    private record RenameTarget(String sourceName, String targetName) {
    }

    private record MoveDocumentTarget(List<String> documentNames, String categoryName) {
    }

    public record ToolExecutionResult(List<ToolCallResponse> toolCalls, String context) {
    }
}
