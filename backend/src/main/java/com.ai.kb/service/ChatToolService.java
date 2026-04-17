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
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatToolService {

    private static final String KB = "\u77e5\u8bc6\u5e93";
    private static final String DOC = "\u6587\u6863";
    private static final String FILE = "\u6587\u4ef6";
    private static final String CATEGORY = "\u5206\u7c7b";
    private static final String CREATE = "\u521b\u5efa";
    private static final String NEW = "\u65b0\u5efa";
    private static final String ADD = "\u65b0\u589e";
    private static final String BUILD = "\u5efa\u7acb";
    private static final String LIST = "\u5217\u8868";
    private static final String LIST_OUT = "\u5217\u51fa";
    private static final String VIEW = "\u67e5\u770b";
    private static final String LOOK = "\u770b\u770b";
    private static final String WHAT = "\u6709\u54ea\u4e9b";
    private static final String MY = "\u6211\u7684";

    private static final Pattern QUOTED_NAME_PATTERN = Pattern.compile("[\"'“”‘’《》「」『』](.+?)[\"'“”‘’《》「」『』]");
    private static final Pattern NAMED_ENTITY_PATTERN = Pattern.compile(
            "(?:\u53eb|\u540d\u4e3a|\u540d\u79f0\u662f|\u540d\u5b57\u662f|\u4e3a)\\s*[:：]?\\s*([^，。！？\\n]+)"
    );

    private final KnowledgeBaseService knowledgeBaseService;
    private final CategoryService categoryService;
    private final DocumentService documentService;

    public ToolExecutionResult execute(String message, String userId, String knowledgeBaseId) {
        String normalized = normalizeMessage(message);
        List<ToolCallResponse> toolCalls = new ArrayList<>();
        List<String> toolFacts = new ArrayList<>();

        if (wantsKnowledgeBaseList(normalized)) {
            List<KnowledgeBaseResponse> list = knowledgeBaseService.getListByUser(userId);
            toolCalls.add(new ToolCallResponse(
                    "list_knowledge_bases",
                    "\u5217\u51fa\u77e5\u8bc6\u5e93",
                    "success",
                    list.isEmpty() ? "\u5f53\u524d\u8fd8\u6ca1\u6709\u77e5\u8bc6\u5e93\u3002" : "\u5df2\u627e\u5230 " + list.size() + " \u4e2a\u77e5\u8bc6\u5e93\u3002",
                    formatKnowledgeBases(list)
            ));
            toolFacts.add("\u77e5\u8bc6\u5e93\u5217\u8868:\n" + formatKnowledgeBases(list));
        }

        if (wantsKnowledgeBaseCreate(normalized)) {
            String name = extractName(message, "\u65b0\u77e5\u8bc6\u5e93");
            try {
                KnowledgeBaseResponse created = knowledgeBaseService.create(userId, new KnowledgeBaseRequest(name, ""));
                toolCalls.add(new ToolCallResponse(
                        "create_knowledge_base",
                        "\u521b\u5efa\u77e5\u8bc6\u5e93",
                        "success",
                        "\u5df2\u521b\u5efa\u77e5\u8bc6\u5e93\u300a" + created.getName() + "\u300b\u3002",
                        "ID: " + created.getId() + "\n\u540d\u79f0: " + created.getName()
                ));
                toolFacts.add("\u521a\u521b\u5efa\u4e86\u77e5\u8bc6\u5e93\u300a" + created.getName() + "\u300b\uff0cID \u4e3a " + created.getId() + "\u3002");
            } catch (Exception e) {
                toolCalls.add(new ToolCallResponse(
                        "create_knowledge_base",
                        "\u521b\u5efa\u77e5\u8bc6\u5e93",
                        "failed",
                        "\u521b\u5efa\u77e5\u8bc6\u5e93\u5931\u8d25\u3002",
                        safeMessage(e)
                ));
                toolFacts.add("\u521b\u5efa\u77e5\u8bc6\u5e93\u5931\u8d25: " + safeMessage(e));
            }
        }

        if (knowledgeBaseId != null && !knowledgeBaseId.isBlank() && wantsDocumentList(normalized)) {
            DocumentListResponse documents = documentService.getList(knowledgeBaseId, null, 1, 20);
            toolCalls.add(new ToolCallResponse(
                    "list_documents",
                    "\u5217\u51fa\u6587\u6863",
                    "success",
                    documents.list().isEmpty() ? "\u5f53\u524d\u77e5\u8bc6\u5e93\u4e0b\u8fd8\u6ca1\u6709\u6587\u6863\u3002" : "\u5df2\u627e\u5230 " + documents.list().size() + " \u7bc7\u6587\u6863\u3002",
                    formatDocuments(documents.list())
            ));
            toolFacts.add("\u5f53\u524d\u77e5\u8bc6\u5e93\u6587\u6863:\n" + formatDocuments(documents.list()));
        }

        if (knowledgeBaseId != null && !knowledgeBaseId.isBlank() && wantsCategoryList(normalized)) {
            List<CategoryResponse> categories = categoryService.getListByKnowledgeBase(knowledgeBaseId);
            toolCalls.add(new ToolCallResponse(
                    "list_categories",
                    "\u5217\u51fa\u5206\u7c7b",
                    "success",
                    categories.isEmpty() ? "\u5f53\u524d\u77e5\u8bc6\u5e93\u4e0b\u8fd8\u6ca1\u6709\u5206\u7c7b\u3002" : "\u5df2\u627e\u5230 " + categories.size() + " \u4e2a\u5206\u7c7b\u3002",
                    formatCategories(categories)
            ));
            toolFacts.add("\u5f53\u524d\u77e5\u8bc6\u5e93\u5206\u7c7b:\n" + formatCategories(categories));
        }

        if (knowledgeBaseId != null && !knowledgeBaseId.isBlank() && wantsCategoryCreate(normalized)) {
            String categoryName = extractName(message, "\u672a\u547d\u540d\u5206\u7c7b");
            try {
                CategoryResponse created = categoryService.create(knowledgeBaseId, new CategoryRequest(categoryName, null));
                toolCalls.add(new ToolCallResponse(
                        "create_category",
                        "\u521b\u5efa\u5206\u7c7b",
                        "success",
                        "\u5df2\u521b\u5efa\u5206\u7c7b\u300a" + created.name() + "\u300b\u3002",
                        "ID: " + created.id() + "\n\u540d\u79f0: " + created.name()
                ));
                toolFacts.add("\u521a\u521b\u5efa\u4e86\u5206\u7c7b\u300a" + created.name() + "\u300b\uff0cID \u4e3a " + created.id() + "\u3002");
            } catch (Exception e) {
                toolCalls.add(new ToolCallResponse(
                        "create_category",
                        "\u521b\u5efa\u5206\u7c7b",
                        "failed",
                        "\u521b\u5efa\u5206\u7c7b\u5931\u8d25\u3002",
                        safeMessage(e)
                ));
                toolFacts.add("\u521b\u5efa\u5206\u7c7b\u5931\u8d25: " + safeMessage(e));
            }
        }

        return new ToolExecutionResult(toolCalls, String.join("\n\n", toolFacts));
    }

    private boolean wantsKnowledgeBaseList(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, WHAT, LIST, LIST_OUT, LOOK, VIEW, MY);
    }

    private boolean wantsKnowledgeBaseCreate(String normalized) {
        return normalized.contains(KB) && containsAny(normalized, CREATE, NEW, ADD, BUILD);
    }

    private boolean wantsDocumentList(String normalized) {
        return containsAny(normalized, DOC, FILE) && containsAny(normalized, WHAT, LIST, LIST_OUT, LOOK, VIEW);
    }

    private boolean wantsCategoryList(String normalized) {
        return normalized.contains(CATEGORY) && containsAny(normalized, WHAT, LIST, LIST_OUT, LOOK, VIEW);
    }

    private boolean wantsCategoryCreate(String normalized) {
        return normalized.contains(CATEGORY) && containsAny(normalized, CREATE, NEW, ADD, BUILD);
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

    private String extractName(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        Matcher quoted = QUOTED_NAME_PATTERN.matcher(message);
        if (quoted.find()) {
            return cleanupName(quoted.group(1), fallback);
        }
        Matcher named = NAMED_ENTITY_PATTERN.matcher(message);
        if (named.find()) {
            return cleanupName(named.group(1), fallback);
        }
        return fallback;
    }

    private String cleanupName(String raw, String fallback) {
        String cleaned = raw == null ? "" : raw.trim();
        cleaned = cleaned.replaceAll("^(?:" + KB + "|" + CATEGORY + ")\\s*", "").trim();
        cleaned = cleaned.replaceAll("\\s*(?:" + KB + "|" + CATEGORY + ")$", "").trim();
        if (cleaned.isBlank()) {
            return fallback;
        }
        return cleaned.length() > 60 ? cleaned.substring(0, 60).trim() : cleaned;
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

    public record ToolExecutionResult(List<ToolCallResponse> toolCalls, String context) {
    }
}
