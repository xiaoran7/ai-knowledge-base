<template>
  <div class="task-center page-container">
    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">Task Center</p>
        <h2>集中管理文档解析、总结和重试任务</h2>
        <p class="hero-copy">
          这里展示当前知识库最近的处理记录。你可以筛选进行中的任务、取消后台处理，或直接跳回文档工作台查看对应资产。
        </p>
      </div>

      <div class="hero-actions">
        <el-button @click="router.push('/documents')">返回文档管理</el-button>
        <el-button type="primary" :loading="loading" @click="fetchTasks">
          刷新任务
        </el-button>
      </div>
    </section>

    <el-card class="toolbar-card">
      <div class="toolbar-grid">
        <div class="toolbar-item">
          <span class="toolbar-label">知识库</span>
          <el-select v-model="selectedKbId" placeholder="选择知识库" @change="handleKbChange">
            <el-option
              v-for="kb in knowledgeStore.knowledgeBaseList"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </div>

        <div class="toolbar-item">
          <span class="toolbar-label">任务状态</span>
          <el-select v-model="statusFilter" placeholder="全部状态">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <section class="summary-grid">
      <el-card class="summary-card">
        <span class="summary-label">总任务数</span>
        <strong>{{ filteredTasks.length }}</strong>
        <p>当前筛选条件下可见的最近任务记录。</p>
      </el-card>
      <el-card class="summary-card">
        <span class="summary-label">进行中</span>
        <strong>{{ processingCount }}</strong>
        <p>支持直接取消后台处理，避免长任务一直占用资源。</p>
      </el-card>
      <el-card class="summary-card">
        <span class="summary-label">失败 / 已取消</span>
        <strong>{{ exceptionCount }}</strong>
        <p>失败和主动取消的任务会集中显示，便于回看原因。</p>
      </el-card>
    </section>

    <el-card class="list-card" v-loading="loading">
      <template v-if="!selectedKbId">
        <el-empty description="请先选择一个知识库" />
      </template>
      <template v-else-if="filteredTasks.length === 0">
        <el-empty description="当前没有符合条件的任务记录" />
      </template>
      <template v-else>
        <div class="task-list">
          <article v-for="task in filteredTasks" :key="task.id" class="task-card">
            <div class="task-card-head">
              <div>
                <div class="task-title">{{ task.documentTitle }}</div>
                <div class="task-meta">
                  <span>{{ getTaskTypeText(task.taskType) }}</span>
                  <span>{{ formatTime(task.createdAt) }}</span>
                  <span v-if="task.completedAt">结束于 {{ formatTime(task.completedAt) }}</span>
                </div>
              </div>

              <div class="task-card-tags">
                <el-tag :type="getTaskStatusTag(task.status)" effect="dark">
                  {{ getTaskStatusText(task.status) }}
                </el-tag>
                <el-tag v-if="task.summaryMode" size="small" effect="plain">
                  {{ getSummaryTypeText(task.summaryMode) }}
                </el-tag>
              </div>
            </div>

            <div class="task-stage">
              当前阶段：{{ getProcessingStageText(task.processingStage) }}
            </div>

            <div v-if="task.errorMessage" class="task-error">
              {{ task.errorMessage }}
            </div>

            <div class="task-actions">
              <el-button link type="primary" @click="openDocument(task.documentId)">
                前往文档
              </el-button>
              <el-button
                v-if="task.status === 'PROCESSING'"
                link
                type="warning"
                :loading="actingTaskId === task.id"
                @click="handleCancel(task.id)"
              >
                取消处理
              </el-button>
              <el-button
                v-if="task.status === 'FAILED' || task.status === 'CANCELED'"
                link
                type="success"
                :loading="actingTaskId === task.id"
                @click="handleRetry(task.documentId)"
              >
                重新处理
              </el-button>
            </div>
          </article>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import {
  cancelDocumentTask,
  getDocumentTaskList,
  retryDocumentProcessing,
  type DocumentTask
} from '@/api/document'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const selectedKbId = ref('')
const statusFilter = ref<'ALL' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CANCELED'>('ALL')
const loading = ref(false)
const actingTaskId = ref('')
const taskList = ref<DocumentTask[]>([])
let pollingTimer: number | null = null

const statusOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '进行中', value: 'PROCESSING' },
  { label: '已完成', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' }
] as const

const filteredTasks = computed(() => {
  if (statusFilter.value === 'ALL') {
    return taskList.value
  }
  return taskList.value.filter((task) => task.status === statusFilter.value)
})

const processingCount = computed(() => taskList.value.filter((task) => task.status === 'PROCESSING').length)
const exceptionCount = computed(() =>
  taskList.value.filter((task) => task.status === 'FAILED' || task.status === 'CANCELED').length
)

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function getTaskTypeText(taskType?: string) {
  const map: Record<string, string> = {
    INGEST: '上传入库',
    SUMMARY: '生成总结',
    RETRY: '重试处理'
  }
  return taskType ? (map[taskType] || taskType) : '未知任务'
}

function getTaskStatusText(status?: string) {
  const map: Record<string, string> = {
    PROCESSING: '进行中',
    SUCCESS: '已完成',
    FAILED: '失败',
    CANCELED: '已取消'
  }
  return status ? (map[status] || status) : '未知状态'
}

function getTaskStatusTag(status?: string) {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PROCESSING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
    CANCELED: 'info'
  }
  return status ? (map[status] || 'info') : 'info'
}

function getSummaryTypeText(summaryType?: string) {
  const map: Record<string, string> = {
    AI_GENERATED: 'AI 自动总结',
    HEURISTIC: '启发式摘要',
    MANUAL_EDITED: '人工编辑摘要',
    EMPTY: '待补充'
  }
  return summaryType ? (map[summaryType] || summaryType) : '未指定'
}

function getProcessingStageText(stage?: string) {
  const map: Record<string, string> = {
    UPLOADED: '已上传',
    PARSING: '解析中',
    SUMMARIZING: '生成总结中',
    INDEXING: '建立索引中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELED: '已取消'
  }
  return stage ? (map[stage] || stage) : '未开始'
}

function stopPolling() {
  if (pollingTimer) {
    window.clearTimeout(pollingTimer)
    pollingTimer = null
  }
}

function schedulePolling() {
  stopPolling()
  if (!taskList.value.some((task) => task.status === 'PROCESSING')) {
    return
  }
  pollingTimer = window.setTimeout(async () => {
    await fetchTasks(false)
    schedulePolling()
  }, 3000)
}

async function fetchTasks(showLoading = true) {
  if (!selectedKbId.value) {
    taskList.value = []
    stopPolling()
    return
  }

  if (showLoading) {
    loading.value = true
  }
  try {
    const response = await getDocumentTaskList({ knowledgeBaseId: selectedKbId.value })
    taskList.value = response.list
    schedulePolling()
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

async function handleKbChange() {
  knowledgeStore.setCurrentKb(selectedKbId.value)
  await fetchTasks()
}

function openDocument(documentId: string) {
  router.push({ path: '/documents', query: { documentId, openPreview: '1' } })
}

async function handleCancel(taskId: string) {
  actingTaskId.value = taskId
  try {
    await cancelDocumentTask(taskId)
    ElMessage.success('任务已取消，后台会在最近的检查点停止')
    await fetchTasks(false)
  } finally {
    actingTaskId.value = ''
  }
}

async function handleRetry(documentId: string) {
  actingTaskId.value = documentId
  try {
    await retryDocumentProcessing(documentId)
    ElMessage.success('已重新加入后台处理队列')
    await fetchTasks(false)
  } finally {
    actingTaskId.value = ''
  }
}

watch(
  () => knowledgeStore.currentKbId,
  async (value) => {
    if (!value) {
      return
    }
    selectedKbId.value = value
    await fetchTasks()
  }
)

onMounted(async () => {
  await knowledgeStore.fetchKnowledgeBaseList()
  selectedKbId.value = knowledgeStore.currentKbId
  if (selectedKbId.value) {
    await fetchTasks()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.task-center {
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
    radial-gradient(circle at top left, rgba(168, 232, 255, 0.38), transparent 36%),
    radial-gradient(circle at bottom right, rgba(255, 217, 153, 0.28), transparent 32%),
    linear-gradient(135deg, #f7fbff 0%, #fefcf7 52%, #f4f8ff 100%);
  border: 1px solid rgba(213, 225, 241, 0.9);
}

.hero-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #5d7894;
}

.hero-panel h2 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  color: #1d2d3d;
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
.list-card,
.summary-card {
  border-radius: 24px;
}

.toolbar-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 320px));
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  color: #203246;
}

.summary-card p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #6a7d92;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.task-card {
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid #e3edf8;
  background: linear-gradient(180deg, #fff 0%, #f7fbff 100%);
}

.task-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.task-title {
  font-size: 16px;
  font-weight: 700;
  color: #203246;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: #73869a;
  font-size: 12px;
}

.task-card-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.task-stage {
  margin-top: 14px;
  color: #365f90;
  font-size: 13px;
}

.task-error {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff4f4;
  border: 1px solid #f2cccc;
  color: #ad4f4f;
  line-height: 1.7;
  font-size: 12px;
}

.task-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 960px) {
  .hero-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .task-card-head {
    flex-direction: column;
  }

  .task-card-tags {
    justify-content: flex-start;
  }
}
</style>
