package com.ai.kb.service;

import com.ai.kb.dto.KnowledgeGraphEdgeResponse;
import com.ai.kb.dto.KnowledgeGraphNodeResponse;
import com.ai.kb.dto.KnowledgeGraphResponse;
import com.ai.kb.dto.KnowledgeGraphStatsResponse;
import com.ai.kb.entity.Category;
import com.ai.kb.entity.Document;
import com.ai.kb.entity.KnowledgeBase;
import com.ai.kb.repository.CategoryRepository;
import com.ai.kb.repository.DocumentRepository;
import com.ai.kb.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private static final String CATEGORY_NODE_PREFIX = "category:";
    private static final String UNCATEGORIZED_NODE_ID = "category:uncategorized";
    private static final Pattern WIKILINK_PATTERN = Pattern.compile("\\[\\[([^\\]|#]+)(?:\\|[^\\]]+)?]]");
    private static final Set<String> DIRECT_TEXT_TYPES = Set.of(
            "md", "markdown", "txt", "csv", "json", "xml", "yaml", "yml", "html", "htm", "java", "js", "ts", "vue"
    );

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;

    public KnowledgeGraphResponse getKnowledgeGraph(String kbId) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));

        List<Document> documents = new ArrayList<>(documentRepository.findByKnowledgeBaseId(kbId));
        List<Category> categories = new ArrayList<>(categoryRepository.findByKnowledgeBaseId(kbId));

        documents.sort(Comparator.comparing(Document::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        categories.sort(Comparator.comparing(Category::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        Map<String, Category> categoryById = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        Map<String, MutableNode> nodes = new LinkedHashMap<>();
        Map<String, MutableEdge> edges = new LinkedHashMap<>();

        for (Category category : categories) {
            String nodeId = buildCategoryNodeId(category.getId());
            nodes.put(nodeId, MutableNode.category(nodeId, category.getName(), false));
            if (category.getParentId() != null && !category.getParentId().isBlank()) {
                addEdge(edges, nodes, buildCategoryNodeId(category.getParentId()), nodeId, "CATEGORY_TREE", "分类层级", 1.0);
            }
        }

        boolean hasUncategorized = documents.stream().anyMatch(document -> document.getCategoryId() == null || document.getCategoryId().isBlank());
        if (hasUncategorized) {
            nodes.put(UNCATEGORIZED_NODE_ID, MutableNode.category(UNCATEGORIZED_NODE_ID, "未分类", true));
        }

        Map<String, Set<String>> aliasToDocumentIds = buildAliasToDocumentIds(documents);
        for (Document document : documents) {
            String categoryName = null;
            if (document.getCategoryId() != null && categoryById.containsKey(document.getCategoryId())) {
                categoryName = categoryById.get(document.getCategoryId()).getName();
            } else if (document.getCategoryId() == null || document.getCategoryId().isBlank()) {
                categoryName = "未分类";
            }

            nodes.put(document.getId(), MutableNode.document(
                    document.getId(),
                    document.getTitle(),
                    document.getCategoryId(),
                    categoryName,
                    document.getStatus(),
                    document.getSummaryType(),
                    safeTags(document.getTags())
            ));

            String categoryNodeId = (document.getCategoryId() == null || document.getCategoryId().isBlank())
                    ? UNCATEGORIZED_NODE_ID
                    : buildCategoryNodeId(document.getCategoryId());
            if (nodes.containsKey(categoryNodeId)) {
                addEdge(edges, nodes, categoryNodeId, document.getId(), "CATEGORY_MEMBERSHIP", "归属分类", 0.9);
            }
        }

        for (Document document : documents) {
            collectReferenceEdges(document, aliasToDocumentIds, edges, nodes);
        }

        for (int i = 0; i < documents.size(); i++) {
            for (int j = i + 1; j < documents.size(); j++) {
                addSharedTagEdge(documents.get(i), documents.get(j), edges, nodes);
            }
        }

        finalizeDegrees(nodes, edges.values());

        List<KnowledgeGraphNodeResponse> nodeResponses = nodes.values().stream()
                .sorted(Comparator
                        .comparing(MutableNode::type)
                        .thenComparing(MutableNode::title, String.CASE_INSENSITIVE_ORDER))
                .map(MutableNode::toResponse)
                .toList();

        List<KnowledgeGraphEdgeResponse> edgeResponses = edges.values().stream()
                .sorted(Comparator
                        .comparing(MutableEdge::type)
                        .thenComparing(MutableEdge::source)
                        .thenComparing(MutableEdge::target))
                .map(MutableEdge::toResponse)
                .toList();

        int documentNodeCount = (int) nodeResponses.stream().filter(node -> "DOCUMENT".equals(node.type())).count();
        int categoryNodeCount = (int) nodeResponses.stream().filter(node -> "CATEGORY".equals(node.type())).count();
        int referenceEdgeCount = (int) edgeResponses.stream().filter(edge -> "REFERENCE".equals(edge.type())).count();
        int sharedTagEdgeCount = (int) edgeResponses.stream().filter(edge -> "SHARED_TAG".equals(edge.type())).count();
        int membershipEdgeCount = (int) edgeResponses.stream().filter(edge -> "CATEGORY_MEMBERSHIP".equals(edge.type())).count();
        int orphanDocumentCount = (int) nodeResponses.stream()
                .filter(node -> "DOCUMENT".equals(node.type()) && Objects.equals(node.degree(), 1))
                .count();

        KnowledgeGraphStatsResponse stats = new KnowledgeGraphStatsResponse(
                nodeResponses.size(),
                edgeResponses.size(),
                documentNodeCount,
                categoryNodeCount,
                referenceEdgeCount,
                sharedTagEdgeCount,
                membershipEdgeCount,
                orphanDocumentCount
        );

        return new KnowledgeGraphResponse(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                nodeResponses,
                edgeResponses,
                stats
        );
    }

    private void collectReferenceEdges(
            Document document,
            Map<String, Set<String>> aliasToDocumentIds,
            Map<String, MutableEdge> edges,
            Map<String, MutableNode> nodes
    ) {
        String content = contentForLinkExtraction(document);
        if (content.isBlank()) {
            return;
        }

        Set<String> resolvedTargets = new LinkedHashSet<>();
        Matcher matcher = WIKILINK_PATTERN.matcher(content);
        while (matcher.find()) {
            resolvedTargets.addAll(resolveCandidates(matcher.group(1), aliasToDocumentIds, document.getId()));
        }

        String normalizedContent = normalize(content);
        for (Map.Entry<String, Set<String>> entry : aliasToDocumentIds.entrySet()) {
            if (entry.getKey().length() < 2 || !normalizedContent.contains(entry.getKey())) {
                continue;
            }
            for (String targetId : entry.getValue()) {
                if (!document.getId().equals(targetId)) {
                    resolvedTargets.add(targetId);
                }
            }
        }

        for (String targetId : resolvedTargets) {
            addEdge(edges, nodes, document.getId(), targetId, "REFERENCE", "文档引用", 1.0);
        }
    }

    private void addSharedTagEdge(
            Document left,
            Document right,
            Map<String, MutableEdge> edges,
            Map<String, MutableNode> nodes
    ) {
        if (hasReferenceEdge(edges, left.getId(), right.getId())) {
            return;
        }

        Set<String> sharedTags = new LinkedHashSet<>(safeTags(left.getTags()));
        sharedTags.retainAll(safeTags(right.getTags()));
        if (sharedTags.isEmpty()) {
            return;
        }

        String source = left.getId().compareTo(right.getId()) <= 0 ? left.getId() : right.getId();
        String target = source.equals(left.getId()) ? right.getId() : left.getId();
        double weight = Math.min(2.5, 0.7 + sharedTags.size() * 0.35);
        String label = "共享标签: " + String.join("、", sharedTags.stream().limit(3).toList());
        addEdge(edges, nodes, source, target, "SHARED_TAG", label, weight);
    }

    private boolean hasReferenceEdge(Map<String, MutableEdge> edges, String leftId, String rightId) {
        return edges.containsKey("REFERENCE:" + leftId + ":" + rightId)
                || edges.containsKey("REFERENCE:" + rightId + ":" + leftId);
    }

    private void addEdge(
            Map<String, MutableEdge> edges,
            Map<String, MutableNode> nodes,
            String source,
            String target,
            String type,
            String label,
            double weight
    ) {
        if (source.equals(target) || !nodes.containsKey(source) || !nodes.containsKey(target)) {
            return;
        }

        String edgeId = type + ":" + source + ":" + target;
        MutableEdge existing = edges.get(edgeId);
        if (existing == null) {
            edges.put(edgeId, new MutableEdge(edgeId, source, target, type, label, weight));
            return;
        }

        existing.weight = Math.max(existing.weight, weight);
        if (label != null && !label.isBlank()) {
            existing.label = label;
        }
    }

    private void finalizeDegrees(Map<String, MutableNode> nodes, Collection<MutableEdge> edges) {
        for (MutableEdge edge : edges) {
            MutableNode source = nodes.get(edge.source);
            MutableNode target = nodes.get(edge.target);
            if (source == null || target == null) {
                continue;
            }
            source.outbound += 1;
            source.degree += 1;
            target.inbound += 1;
            target.degree += 1;
        }
    }

    private Map<String, Set<String>> buildAliasToDocumentIds(List<Document> documents) {
        Map<String, Set<String>> aliasToDocumentIds = new LinkedHashMap<>();
        for (Document document : documents) {
            for (String alias : buildDocumentAliases(document.getTitle())) {
                aliasToDocumentIds.computeIfAbsent(alias, key -> new LinkedHashSet<>()).add(document.getId());
            }
        }
        return aliasToDocumentIds;
    }

    private Set<String> buildDocumentAliases(String title) {
        if (title == null || title.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(normalize(title));

        int dotIndex = title.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < title.length() - 1) {
            String extension = title.substring(dotIndex + 1);
            if (extension.length() <= 6) {
                aliases.add(normalize(title.substring(0, dotIndex)));
            }
        }
        return aliases.stream().filter(alias -> !alias.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> resolveCandidates(String rawTitle, Map<String, Set<String>> aliasToDocumentIds, String selfId) {
        Set<String> resolved = new LinkedHashSet<>();
        for (String alias : buildDocumentAliases(rawTitle)) {
            for (String candidate : aliasToDocumentIds.getOrDefault(alias, Collections.emptySet())) {
                if (!candidate.equals(selfId)) {
                    resolved.add(candidate);
                }
            }
        }
        return resolved;
    }

    private String contentForLinkExtraction(Document document) {
        StringBuilder builder = new StringBuilder();
        if (document.getSummaryContent() != null && !document.getSummaryContent().isBlank()) {
            builder.append(document.getSummaryContent()).append('\n');
        }
        if (document.getParsedContent() != null && !document.getParsedContent().isBlank()) {
            builder.append(document.getParsedContent());
        }
        if (builder.isEmpty()) {
            builder.append(readRawTextContent(document));
        }
        return builder.toString();
    }

    private String readRawTextContent(Document document) {
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            return "";
        }
        if (document.getFileType() == null || !DIRECT_TEXT_TYPES.contains(document.getFileType().toLowerCase(Locale.ROOT))) {
            return "";
        }
        try {
            return Files.readString(Path.of(document.getFilePath()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private String buildCategoryNodeId(String categoryId) {
        return CATEGORY_NODE_PREFIX + categoryId;
    }

    private List<String> safeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("[`*_>#+\\-]+", " ")
                .replaceAll("[\\p{Punct}&&[^.]]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static final class MutableNode {
        private final String id;
        private final String type;
        private final String title;
        private final String categoryId;
        private final String categoryName;
        private final String status;
        private final String summaryType;
        private final List<String> tags;
        private final boolean virtualNode;
        private int degree;
        private int inbound;
        private int outbound;

        private MutableNode(
                String id,
                String type,
                String title,
                String categoryId,
                String categoryName,
                String status,
                String summaryType,
                List<String> tags,
                boolean virtualNode
        ) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.status = status;
            this.summaryType = summaryType;
            this.tags = tags;
            this.virtualNode = virtualNode;
        }

        static MutableNode category(String id, String title, boolean virtualNode) {
            return new MutableNode(id, "CATEGORY", title, null, null, null, null, List.of(), virtualNode);
        }

        static MutableNode document(
                String id,
                String title,
                String categoryId,
                String categoryName,
                String status,
                String summaryType,
                List<String> tags
        ) {
            return new MutableNode(id, "DOCUMENT", title, categoryId, categoryName, status, summaryType, tags, false);
        }

        String type() {
            return type;
        }

        String title() {
            return title;
        }

        KnowledgeGraphNodeResponse toResponse() {
            return new KnowledgeGraphNodeResponse(
                    id,
                    type,
                    title,
                    categoryId,
                    categoryName,
                    status,
                    summaryType,
                    tags,
                    degree,
                    inbound,
                    outbound,
                    virtualNode
            );
        }
    }

    private static final class MutableEdge {
        private final String id;
        private final String source;
        private final String target;
        private final String type;
        private String label;
        private double weight;

        private MutableEdge(String id, String source, String target, String type, String label, double weight) {
            this.id = id;
            this.source = source;
            this.target = target;
            this.type = type;
            this.label = label;
            this.weight = weight;
        }

        String type() {
            return type;
        }

        String source() {
            return source;
        }

        String target() {
            return target;
        }

        KnowledgeGraphEdgeResponse toResponse() {
            return new KnowledgeGraphEdgeResponse(id, source, target, type, label, weight);
        }
    }
}
