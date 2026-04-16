package com.ai.kb.service;

import com.ai.kb.entity.LlmConfig;
import com.ai.kb.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private static final Pattern THINK_PATTERN = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final int DOCUMENT_SUMMARY_CHUNK_SIZE = 6000;
    private static final int DOCUMENT_SUMMARY_CHUNK_OVERLAP = 400;

    private final LlmConfigRepository llmConfigRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GenerationResult chat(String question, String context, LlmConfig config) {
        String systemPrompt = (context != null && !context.isBlank())
                ? buildKnowledgeBasePrompt(context)
                : buildGeneralPrompt();
        return generate(systemPrompt, question, config);
    }

    public GenerationResult chatWithDefaultConfig(String question, String context, String userId) {
        LlmConfig config = llmConfigRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先在设置中配置 LLM API"));

        if (!config.getIsEnabled()) {
            throw new IllegalArgumentException("默认 LLM 配置已禁用");
        }

        return chat(question, context, config);
    }

    public String rewriteQuery(String question, LlmConfig config) {
        String prompt = "Rewrite the user's query into a compact retrieval query. "
                + "Keep it in the same language as the user. Return only the rewritten query.";
        return generate(prompt, question, config).content();
    }

    public String generateConversationTitle(List<String> lines, LlmConfig config) {
        String joined = String.join("\n", lines);
        String prompt = "Generate a short conversation title in Chinese, within 12 characters, based on the conversation. "
                + "Return title only.";
        return generate(prompt, joined, config).content();
    }

    public MemoryResult buildConversationMemory(String sessionSummary, String sessionFacts, List<String> recentMessages, LlmConfig config) {
        String prompt = """
                You update conversation memory for an AI assistant.
                Produce two sections:
                [SUMMARY]
                One short paragraph summarizing the ongoing conversation.
                [FACTS]
                3-8 bullet facts capturing stable user goals, preferences, entities, or decisions.
                Keep the same language as the conversation.
                """;
        String userPrompt = "Existing summary:\n" + nullToEmpty(sessionSummary)
                + "\n\nExisting facts:\n" + nullToEmpty(sessionFacts)
                + "\n\nRecent conversation:\n" + String.join("\n", recentMessages);
        String content = generate(prompt, userPrompt, config).content();

        String summary = content;
        String facts = "";
        int factsIndex = content.indexOf("[FACTS]");
        int summaryIndex = content.indexOf("[SUMMARY]");
        if (summaryIndex >= 0 && factsIndex > summaryIndex) {
            summary = content.substring(summaryIndex + "[SUMMARY]".length(), factsIndex).trim();
            facts = content.substring(factsIndex + "[FACTS]".length()).trim();
        }
        return new MemoryResult(summary.trim(), facts.trim());
    }

    public String summarizeDocument(String title, String content, LlmConfig config) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            return "";
        }

        List<String> chunks = splitDocumentForSummary(normalizedContent, DOCUMENT_SUMMARY_CHUNK_SIZE, DOCUMENT_SUMMARY_CHUNK_OVERLAP);
        if (chunks.size() <= 1) {
            String summary = summarizeWholeDocument(title, normalizedContent, config);
            if (looksTooExtractive(summary, normalizedContent)) {
                return summarizeWholeDocumentStrict(title, normalizedContent, config);
            }
            return summary;
        }

        List<String> sectionSummaries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            sectionSummaries.add(summarizeDocumentChunk(title, chunks.get(i), i + 1, chunks.size(), config));
        }

        String synthesisPrompt = """
                You are preparing a full-document summary for a knowledge base.
                You will receive multiple section summaries extracted from the same document.
                Produce a single markdown summary in Chinese with these sections:
                ## 整体概览
                A 3-5 sentence overview of the full document.
                ## 关键主题
                5-10 concise bullet points covering the full document, not just one section.
                ## 重要概念与实体
                Bullet list of important concepts, APIs, classes, terms, people, tools, or datasets.
                ## 适合检索的问题
                4-8 bullets describing what kinds of questions this document can answer.
                Requirements:
                - Synthesize the whole document instead of repeating every chunk.
                - Preserve terminology from the source.
                - Keep it factual and useful for retrieval.
                - Do not mention chunk numbers in the final answer.
                """;
        String synthesisInput = "文档标题：" + title + "\n\n分段总结如下：\n\n" + String.join("\n\n---\n\n", sectionSummaries);
        String summary = generate(synthesisPrompt, synthesisInput, config).content();
        if (looksTooExtractive(summary, normalizedContent)) {
            return summarizeFromChunkSummariesStrict(title, sectionSummaries, config);
        }
        return summary;
    }

    public GenerationResult generate(String systemPrompt, String userPrompt, LlmConfig config) {
        String raw = callLlmApi(systemPrompt, userPrompt, config);
        return extractThinking(raw);
    }

    private String buildKnowledgeBasePrompt(String context) {
        return "You are a helpful assistant. Prefer the knowledge base content below when answering the user. "
                + "If the retrieved content is not enough, you may supplement with general knowledge, but do not "
                + "invent project-specific facts. Cite or refer to the provided context when possible.\n\n"
                + "Knowledge base content:\n" + context;
    }

    private String buildGeneralPrompt() {
        return "You are a helpful assistant. No relevant knowledge base content was retrieved for this question. "
                + "Answer directly using your general knowledge. If the user asks for project-specific or private "
                + "facts that cannot be verified, say so clearly instead of making them up.";
    }

    private String summarizeWholeDocument(String title, String content, LlmConfig config) {
        String prompt = """
                Summarize the full document for a knowledge base.
                Return markdown in Chinese with these sections:
                ## 整体概览
                A concise 3-5 sentence description of the full document.
                ## 关键主题
                5-10 bullet points.
                ## 重要概念与实体
                Bullet list of important concepts, APIs, classes, terms, people, tools, or datasets.
                ## 适合检索的问题
                4-8 bullets showing what questions the document can answer.
                Keep it factual, retrieval-friendly, and based on the whole document.
                Do not copy long sentences from the original text.
                Avoid quoting more than 12 consecutive characters from the source unless they are API names, class names, commands, or other fixed terms.
                Prefer abstraction, reorganization, and synthesis over paraphrasing line by line.
                """;
        return generate(prompt, "文档标题：" + title + "\n\n文档全文：\n" + content, config).content();
    }

    private String summarizeDocumentChunk(String title, String chunk, int index, int total, LlmConfig config) {
        String prompt = """
                You are summarizing one section of a larger document.
                Return markdown in Chinese with:
                ### 本段摘要
                2-4 sentences summarizing this section.
                ### 本段关键点
                3-6 bullet points.
                ### 本段重要概念
                Bullet list of important concepts, APIs, classes, terms, people, tools, or datasets.
                Focus only on the current section and preserve terminology.
                Do not copy long original sentences verbatim.
                """;
        String userPrompt = "文档标题：" + title
                + "\n当前片段：" + index + "/" + total
                + "\n\n片段内容：\n" + chunk;
        return generate(prompt, userPrompt, config).content();
    }

    private String summarizeWholeDocumentStrict(String title, String content, LlmConfig config) {
        String prompt = """
                You are rewriting an overly extractive document summary into a true abstract for a knowledge base.
                Return markdown in Chinese with these sections:
                ## 整体概览
                Explain the document's purpose, scope, and audience in 3-5 sentences.
                ## 关键主题
                5-8 bullets capturing the main ideas with synthesis.
                ## 重要概念与实体
                Bullet list of important concepts, APIs, classes, terms, people, tools, or datasets.
                ## 适合检索的问题
                4-8 bullets showing what this document helps answer.
                Hard requirements:
                - Do not copy long original sentences.
                - Do not quote more than 12 consecutive characters from the source unless they are fixed technical terms.
                - Summarize by abstraction, grouping, and restructuring.
                - Keep it factual and retrieval-friendly.
                """;
        return generate(prompt, "文档标题：" + title + "\n\n文档全文：\n" + content, config).content();
    }

    private String summarizeFromChunkSummariesStrict(String title, List<String> sectionSummaries, LlmConfig config) {
        String prompt = """
                You are refining a document summary that is still too close to the source.
                Based only on the section summaries, produce a more abstract final summary in Chinese.
                Return markdown with:
                ## 整体概览
                ## 关键主题
                ## 重要概念与实体
                ## 适合检索的问题
                Hard requirements:
                - Do not restate section summaries mechanically.
                - Merge related ideas into higher-level themes.
                - Avoid long quoted spans.
                - Prefer "this document explains / compares / covers" style synthesis.
                """;
        String input = "文档标题：" + title + "\n\n分段总结如下：\n\n" + String.join("\n\n---\n\n", sectionSummaries);
        return generate(prompt, input, config).content();
    }

    private List<String> splitDocumentForSummary(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (content.length() <= chunkSize) {
            chunks.add(content);
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            int breakPoint = findChunkBreakPoint(content, start, end, chunkSize);
            if (breakPoint > start) {
                end = breakPoint;
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

    private int findChunkBreakPoint(String content, int start, int end, int chunkSize) {
        int halfWindow = start + chunkSize / 2;
        int newlineBreak = content.lastIndexOf("\n\n", end);
        if (newlineBreak >= halfWindow) {
            return newlineBreak;
        }
        int lineBreak = content.lastIndexOf('\n', end);
        if (lineBreak >= halfWindow) {
            return lineBreak;
        }
        int sentenceBreak = Math.max(content.lastIndexOf('。', end), content.lastIndexOf('.', end));
        if (sentenceBreak >= halfWindow) {
            return sentenceBreak + 1;
        }
        return end;
    }

    private boolean looksTooExtractive(String summary, String source) {
        if (summary == null || summary.isBlank() || source == null || source.isBlank()) {
            return false;
        }

        String normalizedSummary = normalizeForComparison(summary);
        String normalizedSource = normalizeForComparison(source);
        if (normalizedSummary.isBlank() || normalizedSource.isBlank()) {
            return false;
        }

        int sharedPrefix = longestSharedWindow(normalizedSummary, normalizedSource, 18);
        if (sharedPrefix >= 4) {
            return true;
        }

        String[] lines = normalizedSummary.split("\n");
        int copiedLines = 0;
        int contentLines = 0;
        for (String line : lines) {
            String trimmed = line.replaceFirst("^[-#\\d.\\s]+", "").trim();
            if (trimmed.length() < 18) {
                continue;
            }
            contentLines++;
            if (normalizedSource.contains(trimmed)) {
                copiedLines++;
            }
        }
        return contentLines > 0 && copiedLines * 2 >= contentLines;
    }

    private String normalizeForComparison(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[`*_>#-]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private int longestSharedWindow(String summary, String source, int windowSize) {
        if (summary.length() < windowSize || source.length() < windowSize) {
            return 0;
        }
        int matches = 0;
        for (int i = 0; i <= summary.length() - windowSize; i++) {
            String window = summary.substring(i, i + windowSize);
            if (source.contains(window)) {
                matches++;
            }
        }
        return matches;
    }

    private String callLlmApi(String systemPrompt, String userPrompt, LlmConfig config) {
        String provider = config.getProvider();
        String baseUrl = normalizeBaseUrl(provider, config.getApiBaseUrl());
        String apiKey = config.getApiKey();
        String model = config.getModelName();

        log.info("Calling LLM API: provider={}, model={}, baseUrl={}", provider, model, baseUrl);

        return switch (provider) {
            case "openai", "deepseek", "minimax", "moonshot", "zhipu", "custom" ->
                    callOpenAIFormatApi(provider, baseUrl, apiKey, model, systemPrompt, userPrompt, config);
            case "anthropic" -> callAnthropicApi(baseUrl, apiKey, model, systemPrompt, userPrompt, config);
            case "google" -> callGoogleApi(baseUrl, apiKey, model, systemPrompt, userPrompt, config);
            case "aliyun" -> callAliyunApi(baseUrl, apiKey, model, systemPrompt, userPrompt, config);
            default -> throw new IllegalArgumentException("不支持的 LLM 提供商: " + provider);
        };
    }

    private String callOpenAIFormatApi(String provider, String baseUrl, String apiKey, String model,
                                       String systemPrompt, String userPrompt, LlmConfig config) {
        if (model == null || model.isEmpty()) {
            model = "gpt-3.5-turbo";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.7);
        body.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Exception lastException = null;

        for (String candidateBaseUrl : getOpenAiCompatibleBaseUrls(provider, baseUrl)) {
            try {
                String url = candidateBaseUrl + "/chat/completions";
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return parseOpenAiResponse(response.getBody());
                }
                throw new RuntimeException("LLM API 响应异常: " + response.getStatusCode());
            } catch (RestClientResponseException e) {
                lastException = e;
                int statusCode = e.getStatusCode().value();
                if (shouldRetryMiniMax(provider, candidateBaseUrl, statusCode)) {
                    log.warn("MiniMax request failed on {}, trying next official endpoint. status={}", candidateBaseUrl, statusCode);
                    continue;
                }
                throw new RuntimeException("调用 LLM API 失败: " + e.getMessage());
            } catch (Exception e) {
                lastException = e;
                if (isMiniMaxConfig(provider, candidateBaseUrl)) {
                    log.warn("MiniMax request failed on {}, trying next official endpoint", candidateBaseUrl, e);
                    continue;
                }
                throw new RuntimeException("调用 LLM API 失败: " + e.getMessage());
            }
        }

        throw new RuntimeException("调用 LLM API 失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    private String parseOpenAiResponse(Map<String, Object> responseBody) {
        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new RuntimeException("LLM API 返回格式错误: 缺少 choices");
        }

        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            throw new RuntimeException("LLM API 返回格式错误: 无法解析 choice");
        }

        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            throw new RuntimeException("LLM API 返回格式错误: 缺少 message");
        }

        Object contentObj = message.get("content");
        if (contentObj == null) {
            throw new RuntimeException("LLM API 返回格式错误: 缺少 content");
        }

        return String.valueOf(contentObj);
    }

    private String callAnthropicApi(String baseUrl, String apiKey, String model,
                                    String systemPrompt, String userPrompt, LlmConfig config) {
        String url = baseUrl + "/messages";

        if (model == null || model.isEmpty()) {
            model = "claude-3-haiku-20240307";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);
        body.put("system", systemPrompt);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object contentObj = response.getBody().get("content");
            if (contentObj instanceof List<?> content && !content.isEmpty() && content.get(0) instanceof Map<?, ?> first) {
                return String.valueOf(first.get("text"));
            }
        }
        throw new RuntimeException("Anthropic API 返回格式错误");
    }

    private String callGoogleApi(String baseUrl, String apiKey, String model,
                                 String systemPrompt, String userPrompt, LlmConfig config) {
        if (model == null || model.isEmpty()) {
            model = "gemini-pro";
        }

        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;
        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(Map.of(
                "role", "user",
                "parts", Collections.singletonList(Map.of("text", systemPrompt + "\n\n" + userPrompt))
        ));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        body.put("generationConfig", Map.of(
                "temperature", config.getTemperature() != null ? config.getTemperature() : 0.7,
                "maxOutputTokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096
        ));

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, new HttpHeaders()), Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object candidatesObj = response.getBody().get("candidates");
            if (candidatesObj instanceof List<?> candidates && !candidates.isEmpty() && candidates.get(0) instanceof Map<?, ?> candidate) {
                Object contentObj = candidate.get("content");
                if (contentObj instanceof Map<?, ?> content) {
                    Object partsObj = content.get("parts");
                    if (partsObj instanceof List<?> parts && !parts.isEmpty() && parts.get(0) instanceof Map<?, ?> part) {
                        return String.valueOf(part.get("text"));
                    }
                }
            }
        }
        throw new RuntimeException("Google API 返回格式错误");
    }

    private String callAliyunApi(String baseUrl, String apiKey, String model,
                                 String systemPrompt, String userPrompt, LlmConfig config) {
        String url = baseUrl + "/services/aigc/text-generation/generation";

        if (model == null || model.isEmpty()) {
            model = "qwen-turbo";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", Map.of("messages", messages));
        body.put("parameters", Map.of(
                "temperature", config.getTemperature() != null ? config.getTemperature() : 0.7,
                "max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object outputObj = response.getBody().get("output");
            if (outputObj instanceof Map<?, ?> output) {
                Object textObj = output.get("text");
                if (textObj != null) {
                    return String.valueOf(textObj);
                }
            }
        }
        throw new RuntimeException("阿里云 API 返回格式错误");
    }

    private GenerationResult extractThinking(String rawContent) {
        String thinking = "";
        Matcher matcher = THINK_PATTERN.matcher(rawContent);
        StringBuffer answer = new StringBuffer();
        while (matcher.find()) {
            String block = matcher.group(1).trim();
            if (!block.isBlank()) {
                thinking = thinking.isBlank() ? block : thinking + "\n\n" + block;
            }
            matcher.appendReplacement(answer, "");
        }
        matcher.appendTail(answer);
        String content = answer.toString().trim();
        return new GenerationResult(content.isBlank() ? rawContent.trim() : content, thinking.trim());
    }

    private boolean shouldRetryMiniMax(String provider, String baseUrl, int statusCode) {
        return isMiniMaxConfig(provider, baseUrl) && (statusCode == 401 || statusCode == 403 || statusCode == 404);
    }

    private List<String> getOpenAiCompatibleBaseUrls(String provider, String baseUrl) {
        if (!isMiniMaxConfig(provider, baseUrl)) {
            return List.of(baseUrl);
        }

        Set<String> candidates = new LinkedHashSet<>();
        String normalized = normalizeMiniMaxBaseUrl(baseUrl);
        candidates.add(normalized);
        if (normalized.contains("api.minimaxi.com")) {
            candidates.add("https://api.minimax.io/v1");
        } else if (normalized.contains("api.minimax.io")) {
            candidates.add("https://api.minimaxi.com/v1");
        }
        return new ArrayList<>(candidates);
    }

    private String normalizeBaseUrl(String provider, String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("API baseUrl 不能为空");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return isMiniMaxConfig(provider, normalized) ? normalizeMiniMaxBaseUrl(normalized) : normalized;
    }

    private String normalizeMiniMaxBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("api.minimaxi.com")) {
            normalized = "https://api.minimaxi.com";
        } else if (lower.contains("api.minimax.io")) {
            normalized = "https://api.minimax.io";
        }
        return normalized + "/v1";
    }

    private boolean isMiniMaxConfig(String provider, String baseUrl) {
        if ("minimax".equals(provider)) {
            return true;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        String lower = baseUrl.toLowerCase(Locale.ROOT);
        return lower.contains("minimax.io") || lower.contains("minimaxi.com");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record GenerationResult(String content, String thinking) {
    }

    public record MemoryResult(String summary, String facts) {
    }
}
