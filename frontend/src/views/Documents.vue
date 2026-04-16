<template>
  <div class="documents-page page-container">
    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">Document Intelligence</p>
        <h2>文档预览、解析文本和总结资产分层管理</h2>
        <p class="hero-copy">
          上传后的文档会先进入解析层，再沉淀为可编辑、可切换方式的总结资产。你可以按需选择 AI 自动总结、启发式摘要，或保留人工编辑版本。
        </p>
      </div>

      <div class="hero-actions">
        <el-button class="task-button" @click="handleOpenTasks">
          任务记录
        </el-button>
        <el-upload
          :http-request="handleUpload"
          :show-file-list="false"
          :disabled="!knowledgeStore.currentKbId"
        >
          <el-button type="primary" size="large" class="upload-button">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
        </el-upload>
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
          <span class="toolbar-label">分类</span>
          <el-select
            v-model="selectedCategoryId"
            placeholder="全部分类"
            clearable
            @change="handleCategoryChange"
          >
            <el-option
              v-for="cat in knowledgeStore.categoryList"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading" class="table-card">
      <template v-if="documentList.length === 0">
        <el-empty description="当前分类下还没有文档" />
      </template>

      <template v-else>
        <el-table :data="documentList" style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="240" />
          <el-table-column label="分类" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.categoryName" size="small" effect="plain">
                {{ row.categoryName }}
              </el-tag>
              <span v-else class="muted-text">未分类</span>
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="类型" width="100" />
          <el-table-column label="大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="处理状态" width="140">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="dark">
                {{ getStatusText(row.status) }}
              </el-tag>
              <div v-if="row.status === 'PROCESSING'" class="stage-inline">
                {{ getProcessingStageText(row.processingStage) }}
              </div>
              <div v-if="row.status === 'FAILED' && row.lastError" class="inline-error">
                {{ row.lastError }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="总结方式" width="150">
            <template #default="{ row }">
              <el-tag
                v-if="row.summaryType"
                size="small"
                :type="getSummaryTypeTag(row.summaryType)"
                effect="plain"
              >
                {{ getSummaryTypeText(row.summaryType) }}
              </el-tag>
              <span v-else class="muted-text">待生成</span>
            </template>
          </el-table-column>
          <el-table-column label="总结更新时间" width="190">
            <template #default="{ row }">
              {{ row.summaryUpdatedAt ? formatTime(row.summaryUpdatedAt) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="190">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handlePreview(row.id)">预览 / 编辑</el-button>
              <el-button
                v-if="row.summaryContent"
                link
                type="success"
                @click="handlePreview(row.id, 'summary')"
              >
                查看总结
              </el-button>
              <el-button
                v-if="row.status === 'PROCESSING' || row.status === 'UPLOADED'"
                link
                type="warning"
                @click="refreshDocument(row.id)"
              >
                刷新状态
              </el-button>
              <el-button
                v-if="row.status === 'FAILED' || row.status === 'CANCELED'"
                link
                type="warning"
                @click="handleRetry(row.id)"
              >
                重试处理
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchData"
            @current-change="fetchData"
          />
        </div>
      </template>
    </el-card>

    <el-dialog
      v-model="previewVisible"
      width="1120px"
      top="4vh"
      destroy-on-close
      class="document-dialog"
    >
      <template #header>
        <div class="dialog-header" v-if="previewDocument">
          <div>
            <p class="dialog-overline">Document Workspace</p>
            <h3>{{ previewDocument.title }}</h3>
            <div class="dialog-meta">
              <span>{{ previewDocument.fileType.toUpperCase() }}</span>
              <span>{{ formatFileSize(previewDocument.fileSize) }}</span>
              <span>{{ getStatusText(previewDocument.status) }}</span>
            </div>
          </div>

          <div class="dialog-actions">
            <el-button
              v-if="previewDocument.summaryContent"
              @click="activePreviewTab = 'summary'"
            >
              查看总结
            </el-button>
            <el-button
              v-if="previewDocument.status === 'PROCESSING' || previewDocument.status === 'UPLOADED'"
              @click="refreshDocument(previewDocument.id)"
            >
              刷新状态
            </el-button>
            <el-button
              v-if="previewDocument.status === 'FAILED' || previewDocument.status === 'CANCELED'"
              type="warning"
              plain
              @click="handleRetry(previewDocument.id)"
            >
              重试处理
            </el-button>
            <el-button type="primary" :loading="savingDocument" @click="handleSaveDocument">
              保存文档与总结
            </el-button>
          </div>
        </div>
      </template>

      <template v-if="previewLoading">
        <el-skeleton :rows="10" animated />
      </template>

      <template v-else-if="previewDocument">
        <div class="status-grid">
          <article class="status-card">
            <span class="status-label">处理状态</span>
            <strong>{{ getStatusText(previewDocument.status) }}</strong>
            <p>{{ getStatusDescription(previewDocument.status, previewDocument.processingStage) }}</p>
            <div class="stage-chip">
              当前阶段：{{ getProcessingStageText(previewDocument.processingStage) }}
            </div>
            <div v-if="(previewDocument.status === 'FAILED' || previewDocument.status === 'CANCELED') && previewDocument.lastError" class="error-panel">
              <div class="error-title">失败原因</div>
              <p>{{ previewDocument.lastError }}</p>
              <el-button
                class="retry-button"
                size="small"
                type="warning"
                plain
                @click="handleRetry(previewDocument.id)"
              >
                重试这个文档
              </el-button>
            </div>
          </article>
          <article class="status-card">
            <span class="status-label">当前总结方式</span>
            <strong>{{ getSummaryTypeText(previewDocument.summaryType) }}</strong>
            <p>总结内容单独存储，问答检索默认优先读取总结片段。</p>
          </article>
          <article class="status-card">
            <span class="status-label">最近更新</span>
            <strong>{{ previewDocument.summaryUpdatedAt ? formatTime(previewDocument.summaryUpdatedAt) : '尚未生成' }}</strong>
            <p>你可以切换总结方式，或保留手动修订版本。</p>
          </article>
        </div>

        <el-tabs v-model="activePreviewTab" class="document-tabs">
          <el-tab-pane label="文档预览" name="preview">
            <div class="preview-layout">
              <section class="preview-surface">
                <div class="section-head">
                  <h4>阅读预览</h4>
                  <span>{{ getPreviewModeText(previewDocument.fileType) }}</span>
                </div>

                <template v-if="!editableContent">
                  <el-empty :description="getPreviewEmptyText(previewDocument.status)" />
                </template>
                <template v-else-if="isMarkdownLike(previewDocument.fileType)">
                  <div class="markdown-render" v-html="renderMarkdown(editableContent)"></div>
                </template>
                <template v-else>
                  <pre class="text-render">{{ editableContent }}</pre>
                </template>
              </section>

              <section class="editor-surface">
                <div class="section-head">
                  <h4>解析文本</h4>
                  <span>可编辑</span>
                </div>
                <el-input
                  v-model="editableContent"
                  type="textarea"
                  :rows="22"
                  placeholder="这里保存解析后的文档文本。补充这里的内容后，重新生成总结会基于最新正文。"
                />
              </section>
            </div>
          </el-tab-pane>

          <el-tab-pane label="总结资产" name="summary">
            <div class="summary-layout">
              <section class="summary-preview-panel">
                <div class="section-head">
                  <h4>总结预览</h4>
                  <el-tag :type="getSummaryTypeTag(previewDocument.summaryType)" effect="plain">
                    {{ getSummaryTypeText(previewDocument.summaryType) }}
                  </el-tag>
                </div>

                <div class="summary-mode-card">
                  <div class="summary-mode-title">选择总结方式</div>
                  <el-radio-group v-model="selectedSummaryMode" class="summary-mode-group">
                    <el-radio-button label="AI_GENERATED">AI 自动总结</el-radio-button>
                    <el-radio-button label="HEURISTIC">启发式摘要</el-radio-button>
                    <el-radio-button label="MANUAL_EDITED">人工编辑摘要</el-radio-button>
                  </el-radio-group>

                  <div class="summary-mode-actions">
                    <el-button
                      :loading="summaryActionLoading"
                      type="success"
                      @click="handleGenerateSummary"
                    >
                      按当前方式生成
                    </el-button>
                    <el-button
                      v-if="selectedSummaryMode === 'MANUAL_EDITED'"
                      :loading="summaryActionLoading"
                      @click="handleApplyManualSummary"
                    >
                      保存人工摘要
                    </el-button>
                  </div>

                  <p class="summary-mode-hint">{{ getSummaryModeHint(selectedSummaryMode) }}</p>
                </div>

                <div
                  v-if="editableSummary"
                  class="markdown-render summary-render"
                  v-html="renderMarkdown(editableSummary)"
                ></div>
                <el-empty v-else description="当前还没有总结内容" />
              </section>

              <section class="summary-editor-panel">
                <div class="section-head">
                  <h4>总结内容</h4>
                  <span>独立存储</span>
                </div>
                <el-input
                  v-model="editableSummary"
                  type="textarea"
                  :rows="18"
                  placeholder="这里保存最终总结资产。选中人工编辑摘要时，可以直接在这里改写并保存。"
                />

                <div class="summary-note">
                  <div class="summary-note-title">方式说明</div>
                  <p>AI 自动总结会调用大模型做整体归纳；启发式摘要会快速生成稳定摘要；人工编辑摘要则完全保留你当前写入的内容。</p>
                </div>
              </section>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <el-drawer v-model="taskDrawerVisible" title="文档任务中心" size="720px">
      <div class="task-drawer">
        <div class="task-toolbar">
          <span class="task-toolbar-text">展示当前知识库最近 50 条任务记录，并支持取消进行中的后台处理</span>
          <div class="task-toolbar-actions">
            <el-select v-model="taskStatusFilter" size="small" class="task-filter">
              <el-option label="全部状态" value="ALL" />
              <el-option label="进行中" value="PROCESSING" />
              <el-option label="已完成" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
              <el-option label="已取消" value="CANCELED" />
            </el-select>
            <el-button size="small" @click="fetchTasks">刷新</el-button>
          </div>
        </div>

        <div class="task-summary">
          <article class="task-summary-card">
            <span>总任务</span>
            <strong>{{ filteredTaskList.length }}</strong>
          </article>
          <article class="task-summary-card">
            <span>进行中</span>
            <strong>{{ processingTaskCount }}</strong>
          </article>
          <article class="task-summary-card">
            <span>失败 / 取消</span>
            <strong>{{ issueTaskCount }}</strong>
          </article>
        </div>

        <el-empty v-if="!taskLoading && filteredTaskList.length === 0" description="当前还没有任务记录" />

        <div v-else v-loading="taskLoading" class="task-list">
          <article v-for="task in filteredTaskList" :key="task.id" class="task-card">
            <div class="task-card-head">
              <div>
                <div class="task-title">{{ task.documentTitle }}</div>
                <div class="task-meta">
                  <span>{{ getTaskTypeText(task.taskType) }}</span>
                  <span>{{ formatTime(task.createdAt) }}</span>
                  <span v-if="task.completedAt">结束于 {{ formatTime(task.completedAt) }}</span>
                </div>
              </div>
              <el-tag :type="getTaskStatusTag(task.status)" effect="dark">
                {{ getTaskStatusText(task.status) }}
              </el-tag>
            </div>

            <div class="task-stage">
              当前阶段：{{ getProcessingStageText(task.processingStage) }}
            </div>

            <div v-if="task.summaryMode" class="task-submeta">
              总结方式：{{ getSummaryTypeText(task.summaryMode) }}
            </div>

            <div v-if="task.errorMessage" class="task-error">
              {{ task.errorMessage }}
            </div>

            <div class="task-card-actions">
              <el-button link type="primary" @click="handlePreview(task.documentId)">
                查看文档
              </el-button>
              <el-button
                v-if="task.status === 'CANCELED' || task.status === 'FAILED'"
                link
                type="success"
                @click="handleRetry(task.documentId)"
              >
                重新处理
              </el-button>
              <el-button
                v-if="task.status === 'PROCESSING'"
                link
                type="warning"
                :loading="taskActionLoadingId === task.id"
                @click="handleCancelTask(task.id)"
              >
                取消处理
              </el-button>
            </div>
          </article>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { useKnowledgeStore } from '@/stores/knowledge'
import {
  cancelDocumentTask,
  deleteDocument,
  generateDocumentSummary,
  getDocumentDetail,
  getDocumentList,
  getDocumentTaskList,
  retryDocumentProcessing,
  updateDocument,
  uploadDocument
} from '@/api/document'
import type { Document, DocumentDetail, DocumentStatus, DocumentTask } from '@/api/document'

type SummaryMode = 'AI_GENERATED' | 'HEURISTIC' | 'MANUAL_EDITED'

const knowledgeStore = useKnowledgeStore()
const md = new MarkdownIt({ breaks: true, linkify: true, html: true })

const loading = ref(false)
const documentList = ref<Document[]>([])
const selectedKbId = ref('')
const selectedCategoryId = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const taskDrawerVisible = ref(false)
const taskLoading = ref(false)
const taskList = ref<DocumentTask[]>([])
const taskStatusFilter = ref<'ALL' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CANCELED'>('ALL')
const taskActionLoadingId = ref('')

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewDocument = ref<DocumentDetail | null>(null)
const editableContent = ref('')
const editableSummary = ref('')
const activePreviewTab = ref('preview')
const savingDocument = ref(false)
const summaryActionLoading = ref(false)
const selectedSummaryMode = ref<SummaryMode>('AI_GENERATED')

let pollingTimer: number | null = null

const filteredTaskList = computed(() => {
  if (taskStatusFilter.value === 'ALL') {
    return taskList.value
  }
  return taskList.value.filter((task) => task.status === taskStatusFilter.value)
})

const processingTaskCount = computed(() => taskList.value.filter((task) => task.status === 'PROCESSING').length)
const issueTaskCount = computed(() =>
  taskList.value.filter((task) => task.status === 'FAILED' || task.status === 'CANCELED').length
)

function renderMarkdown(content?: string) {
  if (!content) {
    return '<p class="muted-inline">暂无内容</p>'
  }
  return md.render(content)
}

function formatTime(time: string) {
  return new Date(time).toLocaleString('zh-CN')
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function isMarkdownLike(fileType?: string) {
  return ['md', 'markdown', 'html', 'htm', 'vue'].includes((fileType || '').toLowerCase())
}

function getPreviewModeText(fileType?: string) {
  return isMarkdownLike(fileType) ? 'Markdown 渲染视图' : '文本阅读视图'
}

function getPreviewEmptyText(status: DocumentStatus) {
  if (status === 'PROCESSING' || status === 'UPLOADED') {
    return '文档仍在处理中，解析文本会在处理完成后显示在这里'
  }
  if (status === 'FAILED') {
    return '文档解析失败，可以手动补充解析文本后保存'
  }
  return '当前没有可预览的内容'
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

function getStatusType(status: DocumentStatus) {
  const map: Record<DocumentStatus, 'info' | 'warning' | 'success' | 'danger'> = {
    UPLOADED: 'info',
    PROCESSING: 'warning',
    SUMMARIZED: 'success',
    FAILED: 'danger',
    CANCELED: 'info'
  }
  return map[status]
}

function getStatusText(status: DocumentStatus) {
  const map: Record<DocumentStatus, string> = {
    UPLOADED: '已上传',
    PROCESSING: '处理中',
    SUMMARIZED: '已完成总结',
    FAILED: '处理失败',
    CANCELED: '已取消'
  }
  return map[status]
}

function getStatusDescription(status: DocumentStatus, stage?: string) {
  if (status === 'PROCESSING') {
    const stageMap: Record<string, string> = {
      PARSING: '系统正在抽取原文内容，为后续总结做准备。',
      SUMMARIZING: '系统正在基于解析文本生成总结资产。',
      INDEXING: '系统正在写入分片和向量索引，稍后即可用于问答检索。'
    }
    return stage ? (stageMap[stage] || '系统正在后台处理中。') : '系统正在后台处理中。'
  }
  const map: Record<DocumentStatus, string> = {
    UPLOADED: '文件已经保存，等待进入解析和总结流程。',
    PROCESSING: '系统正在后台处理中。',
    SUMMARIZED: '解析文本和总结资产都已经可以编辑。',
    FAILED: '自动处理失败，但你仍然可以手动补全文本与总结。',
    CANCELED: '本次后台处理已取消，你可以稍后重新发起。'
  }
  return map[status] || '系统正在后台处理中。'
}

function getSummaryTypeText(summaryType?: string) {
  const map: Record<string, string> = {
    AI_GENERATED: 'AI 自动总结',
    HEURISTIC: '启发式摘要',
    MANUAL_EDITED: '人工编辑摘要',
    EMPTY: '待补充'
  }
  return summaryType ? (map[summaryType] || summaryType) : '待生成'
}

function getSummaryTypeTag(summaryType?: string) {
  const map: Record<string, 'success' | 'warning' | 'primary' | 'info'> = {
    AI_GENERATED: 'success',
    HEURISTIC: 'warning',
    MANUAL_EDITED: 'primary',
    EMPTY: 'info'
  }
  return summaryType ? (map[summaryType] || 'info') : 'info'
}

function getSummaryModeHint(summaryMode: SummaryMode) {
  const map: Record<SummaryMode, string> = {
    AI_GENERATED: '调用大模型生成整体总结，适合正式知识资产。',
    HEURISTIC: '快速生成稳定摘要，不依赖 LLM。',
    MANUAL_EDITED: '保留你当前编辑框里的内容作为最终总结。'
  }
  return map[summaryMode]
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
  return status ? (map[status] || status) : '未知'
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

function syncSelectedSummaryMode(summaryType?: string) {
  if (summaryType === 'AI_GENERATED' || summaryType === 'HEURISTIC' || summaryType === 'MANUAL_EDITED') {
    selectedSummaryMode.value = summaryType
    return
  }
  selectedSummaryMode.value = 'AI_GENERATED'
}

function stopPolling() {
  if (pollingTimer) {
    window.clearTimeout(pollingTimer)
    pollingTimer = null
  }
}

function schedulePollingIfNeeded() {
  stopPolling()
  if (!documentList.value.some((item) => item.status === 'UPLOADED' || item.status === 'PROCESSING')) {
    return
  }
  pollingTimer = window.setTimeout(async () => {
    await fetchData(false)
    if (
      previewVisible.value &&
      previewDocument.value &&
      (previewDocument.value.status === 'UPLOADED' || previewDocument.value.status === 'PROCESSING')
    ) {
      await syncPreviewDocument(previewDocument.value.id)
    }
    schedulePollingIfNeeded()
  }, 3000)
}

async function fetchData(showLoading = true) {
  if (!selectedKbId.value) {
    documentList.value = []
    total.value = 0
    return
  }

  if (showLoading) {
    loading.value = true
  }

  try {
    const res = await getDocumentList({
      knowledgeBaseId: selectedKbId.value,
      categoryId: selectedCategoryId.value || undefined,
      page: page.value,
      size: size.value
    })
    documentList.value = res.list
    total.value = res.total
    if (taskDrawerVisible.value) {
      await fetchTasks()
    }
    schedulePollingIfNeeded()
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

async function fetchTasks() {
  if (!selectedKbId.value) {
    taskList.value = []
    return
  }
  taskLoading.value = true
  try {
    const res = await getDocumentTaskList({
      knowledgeBaseId: selectedKbId.value
    })
    taskList.value = res.list
  } finally {
    taskLoading.value = false
  }
}

async function handleOpenTasks() {
  taskDrawerVisible.value = true
  await fetchTasks()
}

async function handleCancelTask(taskId: string) {
  taskActionLoadingId.value = taskId
  try {
    await cancelDocumentTask(taskId)
    ElMessage.success('任务已取消，后台会在最近的检查点停止')
    await fetchTasks()
    await fetchData(false)
  } finally {
    taskActionLoadingId.value = ''
  }
}

async function handleKbChange() {
  knowledgeStore.setCurrentKb(selectedKbId.value)
  selectedCategoryId.value = ''
  page.value = 1
  await knowledgeStore.fetchCategoryList()
  await fetchData()
}

async function handleCategoryChange() {
  page.value = 1
  await fetchData()
}

async function handleUpload({ file }: { file: File }) {
  if (!selectedKbId.value) {
    ElMessage.warning('请先选择知识库')
    return
  }

  const res = await uploadDocument(file, {
    knowledgeBaseId: selectedKbId.value,
    categoryId: selectedCategoryId.value || undefined
  })
  ElMessage.success(`上传成功，当前状态：${getStatusText(res.status)}`)
  await fetchData()
}

function handleDelete(row: Document) {
  ElMessageBox.confirm(`确定删除文档“${row.title}”吗？`, '删除确认', {
    type: 'warning'
  }).then(async () => {
    await deleteDocument(row.id)
    ElMessage.success('文档已删除')
    await fetchData()
  })
}

async function syncPreviewDocument(id: string) {
  previewDocument.value = await getDocumentDetail(id)
  editableContent.value = previewDocument.value.content || ''
  editableSummary.value = previewDocument.value.summaryContent || ''
  syncSelectedSummaryMode(previewDocument.value.summaryType)
}

async function handlePreview(id: string, initialTab: 'preview' | 'summary' = 'preview') {
  previewVisible.value = true
  previewLoading.value = true
  activePreviewTab.value = initialTab
  previewDocument.value = null
  try {
    await syncPreviewDocument(id)
  } finally {
    previewLoading.value = false
  }
}

async function refreshDocument(id: string) {
  if (previewVisible.value && previewDocument.value?.id === id) {
    previewLoading.value = true
    try {
      await syncPreviewDocument(id)
    } finally {
      previewLoading.value = false
    }
  }
  await fetchData(false)
}

async function handleRetry(id: string) {
  summaryActionLoading.value = true
  try {
    const response = await retryDocumentProcessing(id)
    if (previewVisible.value && previewDocument.value?.id === id) {
      previewDocument.value = response
      editableContent.value = response.content || editableContent.value
      editableSummary.value = response.summaryContent || editableSummary.value
      syncSelectedSummaryMode(response.summaryType)
    }
    ElMessage.success('已在后台重试处理，请稍后刷新状态')
    await fetchData(false)
    schedulePollingIfNeeded()
  } finally {
    summaryActionLoading.value = false
  }
}

async function handleSaveDocument() {
  if (!previewDocument.value) {
    return
  }
  savingDocument.value = true
  try {
    previewDocument.value = await updateDocument(previewDocument.value.id, {
      content: editableContent.value,
      summaryContent: editableSummary.value,
      summaryMode: selectedSummaryMode.value
    })
    editableContent.value = previewDocument.value.content || ''
    editableSummary.value = previewDocument.value.summaryContent || ''
    syncSelectedSummaryMode(previewDocument.value.summaryType)
    ElMessage.success('文档内容和总结已保存，检索层已经同步更新')
    await fetchData(false)
  } finally {
    savingDocument.value = false
  }
}

async function handleGenerateSummary() {
  if (!previewDocument.value) {
    return
  }
  summaryActionLoading.value = true
  try {
    previewDocument.value = await generateDocumentSummary(previewDocument.value.id, {
      content: editableContent.value,
      summaryMode: selectedSummaryMode.value
    })
    editableContent.value = previewDocument.value.content || editableContent.value
    syncSelectedSummaryMode(selectedSummaryMode.value)
    ElMessage.success(`${getSummaryTypeText(selectedSummaryMode.value)}已进入后台生成`)
    await fetchData(false)
    schedulePollingIfNeeded()
  } finally {
    summaryActionLoading.value = false
  }
}

async function handleApplyManualSummary() {
  if (!previewDocument.value) {
    return
  }
  summaryActionLoading.value = true
  try {
    previewDocument.value = await updateDocument(previewDocument.value.id, {
      content: editableContent.value,
      summaryContent: editableSummary.value,
      summaryMode: 'MANUAL_EDITED'
    })
    editableContent.value = previewDocument.value.content || ''
    editableSummary.value = previewDocument.value.summaryContent || ''
    syncSelectedSummaryMode(previewDocument.value.summaryType)
    ElMessage.success('人工编辑摘要已保存')
    await fetchData(false)
  } finally {
    summaryActionLoading.value = false
  }
}

watch(
  () => knowledgeStore.currentKbId,
  async (newVal) => {
    if (!newVal) {
      return
    }
    selectedKbId.value = newVal
    selectedCategoryId.value = ''
    page.value = 1
    await knowledgeStore.fetchCategoryList()
    await fetchData()
  }
)

onMounted(async () => {
  await knowledgeStore.fetchKnowledgeBaseList()
  selectedKbId.value = knowledgeStore.currentKbId
  if (selectedKbId.value) {
    await knowledgeStore.fetchCategoryList()
    await fetchData()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.documents-page {
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
    radial-gradient(circle at top left, rgba(255, 214, 153, 0.5), transparent 36%),
    radial-gradient(circle at bottom right, rgba(82, 178, 255, 0.26), transparent 30%),
    linear-gradient(135deg, #fffaf0 0%, #f6fbff 48%, #eef6ff 100%);
  border: 1px solid rgba(232, 221, 198, 0.8);
}

.hero-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #8f6b32;
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
  color: #59708a;
}

.upload-button {
  min-width: 156px;
  height: 48px;
  border-radius: 16px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.task-button {
  min-width: 108px;
  height: 48px;
  border-radius: 16px;
}

.toolbar-card,
.table-card {
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

.toolbar-label {
  font-size: 13px;
  font-weight: 600;
  color: #5e6d80;
}

.muted-text {
  color: #95a2b3;
}

.inline-error {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #d65b5b;
}

.stage-inline {
  margin-top: 6px;
  font-size: 12px;
  color: #6b84a0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.dialog-overline {
  margin: 0 0 6px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: #8b97a8;
}

.dialog-header h3 {
  margin: 0;
  font-size: 24px;
  color: #1a2a3a;
}

.dialog-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  color: #6f8094;
  font-size: 13px;
}

.dialog-actions {
  display: flex;
  gap: 12px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.status-card {
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f7ff 100%);
  border: 1px solid #dce8f6;
}

.status-card strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  color: #203246;
}

.status-card p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #62758b;
}

.stage-chip {
  display: inline-flex;
  margin-top: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #2d6db2;
  font-size: 12px;
  font-weight: 600;
}

.error-panel {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff3f3;
  border: 1px solid #f2c7c7;
}

.error-title {
  font-size: 12px;
  font-weight: 700;
  color: #c24747;
}

.error-panel p {
  margin: 6px 0 0;
  color: #a44848;
}

.retry-button {
  margin-top: 10px;
}

.status-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7f91a5;
}

.document-tabs {
  margin-top: 10px;
}

.preview-layout,
.summary-layout {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 18px;
}

.preview-surface,
.editor-surface,
.summary-preview-panel,
.summary-editor-panel {
  min-height: 520px;
  padding: 20px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e6eef7;
  box-shadow: 0 16px 34px rgba(28, 55, 90, 0.06);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head h4 {
  margin: 0;
  font-size: 17px;
  color: #1f3040;
}

.section-head span {
  font-size: 12px;
  color: #7e90a4;
}

.summary-mode-card {
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f5fbff 0%, #fffdf8 100%);
  border: 1px solid #d7e5f7;
}

.summary-mode-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #29415f;
}

.summary-mode-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-mode-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.summary-mode-hint {
  margin: 12px 0 0;
  line-height: 1.7;
  color: #66798f;
  font-size: 13px;
}

.markdown-render {
  line-height: 1.8;
  color: #223244;
}

.markdown-render :deep(h1),
.markdown-render :deep(h2),
.markdown-render :deep(h3) {
  color: #162536;
}

.markdown-render :deep(p),
.markdown-render :deep(li) {
  color: #32465d;
}

.text-render {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #25384a;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.summary-render {
  min-height: 240px;
}

.summary-note {
  margin-top: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff7ea 0%, #fffdf8 100%);
  border: 1px solid #f3dfb2;
}

.summary-note-title {
  font-size: 13px;
  font-weight: 700;
  color: #8c6525;
}

.summary-note p {
  margin: 8px 0 0;
  line-height: 1.7;
  color: #7a6848;
}

.task-drawer {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.task-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.task-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-filter {
  width: 120px;
}

.task-toolbar-text {
  font-size: 13px;
  color: #6e8197;
}

.task-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.task-summary-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f5f9ff 100%);
  border: 1px solid #e1ebf7;
}

.task-summary-card span {
  font-size: 12px;
  color: #7d8ea4;
}

.task-summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: #203246;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.task-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid #e4edf7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.task-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.task-title {
  font-size: 15px;
  font-weight: 700;
  color: #203246;
}

.task-meta,
.task-submeta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 12px;
  color: #73869a;
}

.task-stage {
  margin-top: 10px;
  font-size: 13px;
  color: #365f90;
}

.task-error {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff3f3;
  border: 1px solid #f2cbcb;
  color: #b04d4d;
  font-size: 12px;
  line-height: 1.6;
}

.task-card-actions {
  margin-top: 10px;
}

.muted-inline {
  color: #93a0b2;
}

@media (max-width: 1100px) {
  .preview-layout,
  .summary-layout,
  .status-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-panel,
  .dialog-header {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-grid {
    grid-template-columns: 1fr;
  }

  .dialog-actions {
    justify-content: flex-start;
  }

  .task-summary {
    grid-template-columns: 1fr;
  }

  .task-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .task-toolbar-actions {
    justify-content: space-between;
  }
}
</style>




