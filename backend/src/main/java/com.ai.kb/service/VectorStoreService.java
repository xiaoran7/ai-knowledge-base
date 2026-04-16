package com.ai.kb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VECTOR_KEY_PREFIX = "vector:";
    private static final String VECTOR_INDEX_KEY = "vector_index:";

    /**
     * 存储向量
     * @param chunkId 切片 ID
     * @param content 文本内容
     * @param embedding 向量
     * @param metadata 元数据（文档ID、知识库ID等）
     * @return 存储的向量 ID
     */
    public String storeVector(String chunkId, String content, float[] embedding, Map<String, Object> metadata) {
        String vectorKey = VECTOR_KEY_PREFIX + chunkId;

        // 存储向量数据
        Map<String, Object> vectorData = new HashMap<>();
        vectorData.put("chunkId", chunkId);
        vectorData.put("content", content);
        vectorData.put("embedding", floatArrayToByteArray(embedding));
        vectorData.put("metadata", metadata);

        redisTemplate.opsForHash().putAll(vectorKey, vectorData);

        // 添加到知识库索引
        String kbId = (String) metadata.get("knowledgeBaseId");
        if (kbId != null) {
            String indexKey = VECTOR_INDEX_KEY + kbId;
            redisTemplate.opsForSet().add(indexKey, chunkId);
        }

        log.info("向量存储成功, chunkId: {}", chunkId);
        return chunkId;
    }

    /**
     * 批量存储向量
     */
    public List<String> storeVectors(List<String> chunkIds, List<String> contents, List<float[]> embeddings, List<Map<String, Object>> metadataList) {
        List<String> storedIds = new ArrayList<>();

        for (int i = 0; i < chunkIds.size(); i++) {
            if (embeddings.get(i) != null && embeddings.get(i).length > 0) {
                String id = storeVector(chunkIds.get(i), contents.get(i), embeddings.get(i), metadataList.get(i));
                storedIds.add(id);
            }
        }

        log.info("批量向量存储成功, 数量: {}", storedIds.size());
        return storedIds;
    }

    /**
     * 相似度搜索
     * @param queryEmbedding 查询向量
     * @param knowledgeBaseId 知识库 ID（用于过滤）
     * @param topK 返回数量
     * @return 搜索结果列表
     */
    public List<VectorSearchResult> searchSimilar(float[] queryEmbedding, String knowledgeBaseId, int topK) {
        // 获取知识库中的所有向量 ID
        String indexKey = VECTOR_INDEX_KEY + knowledgeBaseId;
        Set<Object> chunkIds = redisTemplate.opsForSet().members(indexKey);

        if (chunkIds == null || chunkIds.isEmpty()) {
            log.info("知识库 {} 没有向量数据", knowledgeBaseId);
            return Collections.emptyList();
        }

        List<VectorSearchResult> results = new ArrayList<>();

        for (Object chunkIdObj : chunkIds) {
            String chunkId = (String) chunkIdObj;
            String vectorKey = VECTOR_KEY_PREFIX + chunkId;

            Map<Object, Object> vectorData = redisTemplate.opsForHash().entries(vectorKey);
            if (vectorData == null || vectorData.isEmpty()) {
                continue;
            }

            // 获取存储的向量
            byte[] embeddingBytes = (byte[]) vectorData.get("embedding");
            if (embeddingBytes == null) {
                continue;
            }

            float[] storedEmbedding = byteArrayToFloatArray(embeddingBytes);

            // 计算余弦相似度
            double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);

            if (similarity > 0.5) { // 相似度阈值
                String content = (String) vectorData.get("content");
                Map<String, Object> metadata = (Map<String, Object>) vectorData.get("metadata");

                results.add(new VectorSearchResult(chunkId, content, metadata, similarity));
            }
        }

        // 按相似度排序并返回 topK
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        log.info("向量搜索完成, 知识库: {}, 结果数: {}", knowledgeBaseId, results.size());
        return results;
    }

    /**
     * 删除向量
     */
    public void deleteVector(String vectorId) {
        String vectorKey = VECTOR_KEY_PREFIX + vectorId;

        // 先获取元数据以更新索引
        Map<Object, Object> vectorData = redisTemplate.opsForHash().entries(vectorKey);
        if (vectorData != null && !vectorData.isEmpty()) {
            Map<String, Object> metadata = (Map<String, Object>) vectorData.get("metadata");
            if (metadata != null) {
                String kbId = (String) metadata.get("knowledgeBaseId");
                if (kbId != null) {
                    String indexKey = VECTOR_INDEX_KEY + kbId;
                    redisTemplate.opsForSet().remove(indexKey, vectorId);
                }
            }
        }

        redisTemplate.delete(vectorKey);
        log.info("向量删除成功, vectorId: {}", vectorId);
    }

    /**
     * 批量删除向量
     */
    public void deleteVectors(List<String> vectorIds) {
        if (vectorIds != null && !vectorIds.isEmpty()) {
            for (String vectorId : vectorIds) {
                deleteVector(vectorId);
            }
            log.info("批量向量删除成功, 数量: {}", vectorIds.size());
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * float 数组转 byte 数组
     */
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

    /**
     * byte 数组转 float 数组
     */
    private float[] byteArrayToFloatArray(byte[] bytes) {
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = ((bytes[i * 4] & 0xFF) << 24) |
                      ((bytes[i * 4 + 1] & 0xFF) << 16) |
                      ((bytes[i * 4 + 2] & 0xFF) << 8) |
                      (bytes[i * 4 + 3] & 0xFF);
            floats[i] = Float.intBitsToFloat(bits);
        }
        return floats;
    }

    /**
     * 向量搜索结果
     */
    public static class VectorSearchResult {
        private String id;
        private String content;
        private Map<String, Object> metadata;
        private double score;

        public VectorSearchResult(String id, String content, Map<String, Object> metadata, double score) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
            this.score = score;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public double getScore() { return score; }

        public String getDocumentId() {
            return metadata != null ? (String) metadata.get("documentId") : null;
        }

        public String getDocumentTitle() {
            return metadata != null ? (String) metadata.get("documentTitle") : null;
        }

        public String getChunkIndex() {
            return metadata != null ? String.valueOf(metadata.get("chunkIndex")) : null;
        }

        public String getChunkType() {
            return metadata != null ? String.valueOf(metadata.get("chunkType")) : null;
        }
    }
}
