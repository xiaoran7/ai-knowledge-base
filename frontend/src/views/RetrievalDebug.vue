<template>
  <div class="retrieval-workspace page-container">
    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">Retrieval Workspace</p>
        <h2>把 query rewrite、召回结果和命中片段放到同一个工作台里</h2>
        <p class="hero-copy">
          这里适合做检索质量排查。你可以输入问题，查看改写前后的查询词、最终采用的检索语句，以及 summary / raw
          chunk 的命中差异。
        </p>
      </div>

      <div class="hero-actions">
        <el-button @click="router.push('/chat')">返回 AI 问答</el-button>
        <el-button type="primary" :loading="loading" @click="handleRunDebug">开始调试</el-button>
      </div>
    </section>

    <el-card class="toolbar-card">
      <div class="toolbar-grid">
        <div class="toolbar-item">
          <span class="toolbar-label">知识库</span>
          <el-select v-model="selectedKbId" placeholder="选择知识库">
            <el-option
              v-for="kb in knowledgeStore.knowledgeBaseList"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </div>

        <div class="toolbar-item">
          <span class="toolbar-label">Top K</span>
          <el-input-number v-model="topK" :min="1" :max="20" />
        </div>
      </div>

      <div class="query-panel">
        <span class="toolbar-label">调试问题</span>
        <el-input
          v-model="query"
          type="textarea"
          :rows="4"
          placeholder="输入一个用户问题，观察 query rewrite、最终检索词和召回片段"
          @keydown.ctrl.enter.prevent="handleRunDebug"
        />
        <div class="query-actions">
          <span class="query-hint">按 `Ctrl + Enter` 可直接开始调试</span>
          <el-button type="primary" :loading="loading" @click="handleRunDebug">运行检索调试</el-button>
        </div>
      </div>
    </el-card>

    <template v-if="result">
      <section class="summary-grid">
        <el-card class="summary-card">
          <span class="summary-label">原始问题</span>
          <strong>{{ result.originalQuery }}</strong>
          <p>这是用户最初输入到检索链路的问题。</p>
        </el-card>
        <el-card class="summary-card">
          <span class="summary-label">改写问题</span>
          <strong>{{ result.rewrittenQuery || '未触发改写' }}</strong>
          <p>如果改写结果更适合检索，这里会展示 rewrite 后的版本。</p>
        </el-card>
        <el-card class="summary-card">
          <span class="summary-label">最终采用</span>
          <strong>{{ result.usedQuery }}</strong>
          <p>后端最终拿去做向量召回的查询词。</p>
        </el-card>
      </section>

      <el-card class="results-card">
        <div class="result-header">
          <div>
            <h3>召回对比</h3>
            <p>同一问题下，对比原始查询、改写查询和最终选中结果。</p>
          </div>
          <div class="result-stat">
            <el-tag type="success" effect="plain">最终命中 {{ result.finalHits.length }}</el-tag>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="result-tabs">
          <el-tab-pane :label="`最终命中 (${result.finalHits.length})`" name="final">
            <div class="hit-list">
              <article v-for="hit in result.finalHits" :key="`final-${hit.chunkId}`" class="hit-card">
                <div class="hit-head">
                  <div>
                    <div class="hit-title">{{ hit.documentTitle || '未命名文档' }}</div>
                    <div class="hit-meta">
                      <span>chunk {{ hit.chunkIndex }}</span>
                      <span>{{ formatScore(hit.score) }}</span>
                    </div>
                  </div>

                  <div class="hit-tags">
                    <el-tag size="small" :type="getChunkTagType(hit.chunkType)">
                      {{ formatChunkType(hit.chunkType) }}
                    </el-tag>
                    <el-button link type="primary" @click="openDocument(hit.documentId)">
                      前往文档
                    </el-button>
                  </div>
                </div>
                <div class="hit-content">{{ hit.content }}</div>
              </article>
              <el-empty v-if="!result.finalHits.length" description="当前问题没有命中可展示的检索结果" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`原始召回 (${result.originalHits.length})`" name="original">
            <div class="hit-list">
              <article
                v-for="hit in result.originalHits"
                :key="`original-${hit.chunkId}`"
                class="hit-card compact"
              >
                <div class="hit-head">
                  <div>
                    <div class="hit-title">{{ hit.documentTitle || '未命名文档' }}</div>
                    <div class="hit-meta">
                      <span>chunk {{ hit.chunkIndex }}</span>
                      <span>{{ formatScore(hit.score) }}</span>
                    </div>
                  </div>
                  <el-tag size="small" :type="getChunkTagType(hit.chunkType)">
                    {{ formatChunkType(hit.chunkType) }}
                  </el-tag>
                </div>
                <div class="hit-content">{{ hit.content }}</div>
              </article>
              <el-empty v-if="!result.originalHits.length" description="原始问题未命中结果" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`改写召回 (${result.rewrittenHits.length})`" name="rewritten">
            <div class="hit-list">
              <article
                v-for="hit in result.rewrittenHits"
                :key="`rewritten-${hit.chunkId}`"
                class="hit-card compact"
              >
                <div class="hit-head">
                  <div>
                    <div class="hit-title">{{ hit.documentTitle || '未命名文档' }}</div>
                    <div class="hit-meta">
                      <span>chunk {{ hit.chunkIndex }}</span>
                      <span>{{ formatScore(hit.score) }}</span>
                    </div>
                  </div>
                  <el-tag size="small" :type="getChunkTagType(hit.chunkType)">
                    {{ formatChunkType(hit.chunkType) }}
                  </el-tag>
                </div>
                <div class="hit-content">{{ hit.content }}</div>
              </article>
              <el-empty v-if="!result.rewrittenHits.length" description="改写问题未命中结果" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <el-card v-else class="empty-card">
      <el-empty description="先输入一个问题并运行调试，这里会展示检索链路详情" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { debugRetrieval, type RetrievalDebugResponse } from '@/api/chat'
import { useKnowledgeStore } from '@/stores/knowledge'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const selectedKbId = ref('')
const topK = ref(8)
const query = ref('')
const loading = ref(false)
const activeTab = ref<'final' | 'original' | 'rewritten'>('final')
const result = ref<RetrievalDebugResponse | null>(null)

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error && 'message' in error && typeof error.message === 'string') {
    return error.message
  }
  return '检索调试失败，请稍后重试'
}

function formatScore(score?: number) {
  return `${Math.round((score || 0) * 100)}%`
}

function formatChunkType(chunkType?: string) {
  if (!chunkType) {
    return '未知片段'
  }
  return chunkType.toLowerCase() === 'summary' ? '总结片段' : '原文片段'
}

function getChunkTagType(chunkType?: string) {
  return chunkType?.toLowerCase() === 'summary' ? 'success' : 'info'
}

function openDocument(documentId: string) {
  router.push({ path: '/documents', query: { documentId, openPreview: '1' } })
}

async function handleRunDebug() {
  if (!selectedKbId.value) {
    ElMessage.warning('请先选择一个知识库')
    return
  }
  if (!query.value.trim()) {
    ElMessage.warning('请先输入一个调试问题')
    return
  }

  loading.value = true
    try {
      result.value = await debugRetrieval({
        knowledgeBaseId: selectedKbId.value,
        message: query.value.trim(),
        topK: topK.value
      })
      activeTab.value = 'final'
    } catch (error) {
      ElMessage.error(getErrorMessage(error))
    } finally {
      loading.value = false
    }
  }

onMounted(async () => {
  await knowledgeStore.fetchKnowledgeBaseList()
  selectedKbId.value = knowledgeStore.currentKbId
})
</script>

<style scoped>
.retrieval-workspace {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(156, 230, 205, 0.34), transparent 36%),
    radial-gradient(circle at bottom right, rgba(255, 226, 163, 0.3), transparent 34%),
    linear-gradient(135deg, #f5fffb 0%, #fbfcff 55%, #fffaf2 100%);
  border: 1px solid rgba(214, 229, 223, 0.9);
}

.hero-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #567f72;
}

.hero-panel h2 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  color: #1e2f3f;
}

.hero-copy {
  max-width: 760px;
  margin: 12px 0 0;
  line-height: 1.8;
  color: #60748a;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.toolbar-card,
.summary-card,
.results-card,
.empty-card {
  border-radius: 24px;
}

.toolbar-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 280px));
  gap: 18px;
}

.toolbar-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toolbar-label,
.summary-label {
  font-size: 13px;
  font-weight: 600;
  color: #5e6d80;
}

.query-panel {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.query-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.query-hint {
  font-size: 13px;
  color: #72869c;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  line-height: 1.5;
  color: #203246;
  word-break: break-word;
}

.summary-card p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #6a7d92;
}

.result-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 10px;
}

.result-header h3 {
  margin: 0;
  font-size: 22px;
  color: #203246;
}

.result-header p {
  margin: 8px 0 0;
  color: #6c7d92;
}

.hit-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hit-card {
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid #e1ecf4;
  background: linear-gradient(180deg, #fff 0%, #f8fbff 100%);
}

.hit-card.compact {
  background: #fff;
}

.hit-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hit-title {
  font-size: 16px;
  font-weight: 700;
  color: #203246;
}

.hit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  font-size: 12px;
  color: #71849a;
}

.hit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.hit-content {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f5f8fc;
  color: #33485f;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .hero-panel,
  .query-actions,
  .result-header,
  .hit-head {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .hit-tags {
    justify-content: flex-start;
  }
}
</style>
