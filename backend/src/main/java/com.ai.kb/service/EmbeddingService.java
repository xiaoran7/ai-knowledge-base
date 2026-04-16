package com.ai.kb.service;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.ai.kb.entity.LlmConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${embedding.model-dir:models/bge-small-zh-v1.5-onnx}")
    private String modelDir;

    @Value("${embedding.model-file:onnx/model_quantized.onnx}")
    private String modelFile;

    @Value("${embedding.query-instruction:为这个句子生成表示以用于检索相关文章：}")
    private String queryInstruction;

    private OrtEnvironment environment;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;
    private boolean requiresTokenTypeIds;

    @PostConstruct
    public void init() {
        try {
            Path modelBaseDir = resolveModelBaseDir();
            Path modelPath = modelBaseDir.resolve(modelFile);

            if (!Files.exists(modelPath)) {
                throw new IllegalStateException("未找到本地 embedding 模型文件: " + modelPath);
            }

            tokenizer = HuggingFaceTokenizer.newInstance(modelBaseDir);
            environment = OrtEnvironment.getEnvironment();
            session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            requiresTokenTypeIds = session.getInputInfo().containsKey("token_type_ids");

            log.info("本地 embedding 模型加载成功: {}", modelPath);
        } catch (Exception e) {
            throw new IllegalStateException("初始化本地 embedding 模型失败: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (session != null) {
                session.close();
            }
            if (environment != null) {
                environment.close();
            }
            if (tokenizer != null) {
                tokenizer.close();
            }
        } catch (Exception e) {
            log.warn("关闭本地 embedding 资源时出错: {}", e.getMessage());
        }
    }

    public float[] getEmbedding(String text, String userId) {
        return embedQuery(text);
    }

    public float[] getEmbeddingWithConfig(String text, LlmConfig config) {
        return embedQuery(text);
    }

    public List<float[]> getEmbeddings(List<String> texts, LlmConfig config) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            try {
                result.add(embedDocument(text));
            } catch (Exception e) {
                log.error("生成文档 embedding 失败: {}", e.getMessage());
                result.add(new float[0]);
            }
        }
        return result;
    }

    private float[] embedQuery(String text) {
        return infer(queryInstruction + text);
    }

    private float[] embedDocument(String text) {
        return infer(text);
    }

    private float[] infer(String text) {
        try {
            Encoding encoding = tokenizer.encode(text);
            long[] inputIds = toLongArray(encoding.getIds());
            long[] attentionMask = toLongArray(encoding.getAttentionMask());
            long[] typeIds = encoding.getTypeIds() != null ? toLongArray(encoding.getTypeIds()) : new long[inputIds.length];

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", OnnxTensor.createTensor(environment, new long[][] { inputIds }));
            inputs.put("attention_mask", OnnxTensor.createTensor(environment, new long[][] { attentionMask }));
            if (requiresTokenTypeIds) {
                inputs.put("token_type_ids", OnnxTensor.createTensor(environment, new long[][] { typeIds }));
            }

            try (OrtSession.Result result = session.run(inputs)) {
                Object value = result.get(0).getValue();
                if (!(value instanceof float[][][] hiddenStates) || hiddenStates.length == 0 || hiddenStates[0].length == 0) {
                    throw new IllegalStateException("embedding 模型输出格式异常");
                }

                float[] clsEmbedding = hiddenStates[0][0];
                return normalize(clsEmbedding);
            } finally {
                for (OnnxTensor tensor : inputs.values()) {
                    tensor.close();
                }
            }
        } catch (OrtException e) {
            throw new RuntimeException("本地 ONNX embedding 推理失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("获取本地 embedding 失败: " + e.getMessage(), e);
        }
    }

    private Path resolveModelBaseDir() {
        Path configured = Paths.get(modelDir);
        if (configured.isAbsolute()) {
            return configured;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
    }

    private long[] toLongArray(long[] values) {
        return values;
    }

    private long[] toLongArray(List<Long> values) {
        long[] output = new long[values.size()];
        for (int i = 0; i < values.size(); i++) {
            output[i] = values.get(i);
        }
        return output;
    }

    private float[] normalize(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }

        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }
}
