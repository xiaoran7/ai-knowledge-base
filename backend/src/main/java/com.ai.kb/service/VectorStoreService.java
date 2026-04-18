package com.ai.kb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private static final String VECTOR_KEY_PREFIX = "vector:";
    private static final String VECTOR_INDEX_KEY = "vector_index:";

    private final RedisTemplate<String, Object> redisTemplate;

    public String storeVector(String chunkId, String content, float[] embedding, Map<String, Object> metadata) {
        String vectorKey = VECTOR_KEY_PREFIX + chunkId;

        Map<String, Object> vectorData = new HashMap<>();
        vectorData.put("chunkId", chunkId);
        vectorData.put("content", content);
        vectorData.put("embedding", floatArrayToByteArray(embedding));
        vectorData.put("metadata", metadata);
        redisTemplate.opsForHash().putAll(vectorKey, vectorData);

        String kbId = metadata == null ? null : (String) metadata.get("knowledgeBaseId");
        if (kbId != null && !kbId.isBlank()) {
            redisTemplate.opsForSet().add(VECTOR_INDEX_KEY + kbId, chunkId);
        }

        log.info("鍚戦噺瀛樺偍鎴愬姛, chunkId: {}", chunkId);
        return chunkId;
    }

    public List<String> storeVectors(
            List<String> chunkIds,
            List<String> contents,
            List<float[]> embeddings,
            List<Map<String, Object>> metadataList
    ) {
        List<String> storedIds = new ArrayList<>();
        for (int i = 0; i < chunkIds.size(); i++) {
            float[] embedding = embeddings.get(i);
            if (embedding != null && embedding.length > 0) {
                storedIds.add(storeVector(chunkIds.get(i), contents.get(i), embedding, metadataList.get(i)));
            }
        }
        log.info("鎵归噺鍚戦噺瀛樺偍鎴愬姛, 鏁伴噺: {}", storedIds.size());
        return storedIds;
    }

    public List<VectorSearchResult> searchSimilar(float[] queryEmbedding, String knowledgeBaseId, int topK) {
        String indexKey = VECTOR_INDEX_KEY + knowledgeBaseId;
        Set<Object> chunkIds = redisTemplate.opsForSet().members(indexKey);
        if (chunkIds == null || chunkIds.isEmpty()) {
            log.info("鐭ヨ瘑搴?{} 娌℃湁鍚戦噺鏁版嵁", knowledgeBaseId);
            return Collections.emptyList();
        }

        List<VectorSearchResult> results = new ArrayList<>();
        for (Object chunkIdObj : chunkIds) {
            String chunkId = String.valueOf(chunkIdObj);
            Map<Object, Object> vectorData = redisTemplate.opsForHash().entries(VECTOR_KEY_PREFIX + chunkId);
            if (vectorData == null || vectorData.isEmpty()) {
                continue;
            }

            byte[] embeddingBytes = readEmbeddingBytes(vectorData.get("embedding"));
            if (embeddingBytes == null || embeddingBytes.length == 0 || embeddingBytes.length % 4 != 0) {
                continue;
            }

            float[] storedEmbedding = byteArrayToFloatArray(embeddingBytes);
            double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);
            if (similarity <= 0.5d) {
                continue;
            }

            String content = String.valueOf(vectorData.getOrDefault("content", ""));
            Map<String, Object> metadata = readMetadata(vectorData.get("metadata"));
            results.add(new VectorSearchResult(chunkId, content, metadata, similarity));
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (results.size() > topK) {
            return new ArrayList<>(results.subList(0, topK));
        }

        log.info("鍚戦噺鎼滅储瀹屾垚, 鐭ヨ瘑搴?{}, 缁撴灉鏁?{}", knowledgeBaseId, results.size());
        return results;
    }

    public void deleteVector(String vectorId) {
        String vectorKey = VECTOR_KEY_PREFIX + vectorId;
        Map<Object, Object> vectorData = redisTemplate.opsForHash().entries(vectorKey);
        if (vectorData != null && !vectorData.isEmpty()) {
            Map<String, Object> metadata = readMetadata(vectorData.get("metadata"));
            String kbId = metadata == null ? null : (String) metadata.get("knowledgeBaseId");
            if (kbId != null && !kbId.isBlank()) {
                redisTemplate.opsForSet().remove(VECTOR_INDEX_KEY + kbId, vectorId);
            }
        }

        redisTemplate.delete(vectorKey);
        log.info("鍚戦噺鍒犻櫎鎴愬姛, vectorId: {}", vectorId);
    }

    public void deleteVectors(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }
        for (String vectorId : vectorIds) {
            deleteVector(vectorId);
        }
        log.info("鎵归噺鍚戦噺鍒犻櫎鎴愬姛, 鏁伴噺: {}", vectorIds.size());
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0d;
        }

        double dotProduct = 0.0d;
        double normA = 0.0d;
        double normB = 0.0d;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0d || normB == 0.0d) {
            return 0.0d;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private byte[] floatArrayToByteArray(float[] floats) {
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = Float.floatToIntBits(floats[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }

    private float[] byteArrayToFloatArray(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = ((bytes[i * 4] & 0xFF) << 24)
                    | ((bytes[i * 4 + 1] & 0xFF) << 16)
                    | ((bytes[i * 4 + 2] & 0xFF) << 8)
                    | (bytes[i * 4 + 3] & 0xFF);
            floats[i] = Float.intBitsToFloat(bits);
        }
        return floats;
    }

    private byte[] readEmbeddingBytes(Object rawEmbedding) {
        if (rawEmbedding == null) {
            return null;
        }
        if (rawEmbedding instanceof byte[] bytes) {
            return bytes;
        }
        if (rawEmbedding instanceof String base64) {
            try {
                return Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to decode embedding bytes from Base64 string");
                return null;
            }
        }
        if (rawEmbedding instanceof List<?> values) {
            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (!(value instanceof Number number)) {
                    log.warn("Unsupported embedding element type: {}", value == null ? "null" : value.getClass().getName());
                    return null;
                }
                bytes[i] = number.byteValue();
            }
            return bytes;
        }

        log.warn("Unsupported embedding payload type from Redis: {}", rawEmbedding.getClass().getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMetadata(Object rawMetadata) {
        if (rawMetadata instanceof Map<?, ?> map) {
            Map<String, Object> metadata = new HashMap<>();
            map.forEach((key, value) -> metadata.put(String.valueOf(key), value));
            return metadata;
        }
        return Collections.emptyMap();
    }

    public static class VectorSearchResult {
        private final String id;
        private final String content;
        private final Map<String, Object> metadata;
        private final double score;

        public VectorSearchResult(String id, String content, Map<String, Object> metadata, double score) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
            this.score = score;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public double getScore() {
            return score;
        }

        public String getDocumentId() {
            return metadata != null ? stringValue(metadata.get("documentId")) : null;
        }

        public String getDocumentTitle() {
            return metadata != null ? stringValue(metadata.get("documentTitle")) : null;
        }

        public String getChunkIndex() {
            return metadata != null ? stringValue(metadata.get("chunkIndex")) : null;
        }

        public String getChunkType() {
            return metadata != null ? stringValue(metadata.get("chunkType")) : null;
        }

        private String stringValue(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }
}
