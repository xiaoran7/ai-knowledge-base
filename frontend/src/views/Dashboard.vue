<template>
  <div class="dashboard page-container">
    <h2>欢迎使用 AI 知识库</h2>

    <el-row :gutter="20" class="mt-4">
      <el-col :span="8">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-content">
            <el-icon :size="40" color="#409EFF"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ documentCount }}</div>
              <div class="stat-label">文档总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-content">
            <el-icon :size="40" color="#67C23A"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ conversationCount }}</div>
              <div class="stat-label">会话数量</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="stat-card" v-loading="loading">
          <div class="stat-content">
            <el-icon :size="40" color="#E6A23C"><Folder /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ categoryCount }}</div>
              <div class="stat-label">分类数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>快速开始</span>
        </div>
      </template>

      <el-steps :active="activeStep" align-center>
        <el-step title="创建知识库" description="先创建一个知识库容器" />
        <el-step title="上传资料" description="把 PDF、Markdown、Word 等资料放进去" />
        <el-step title="开始问答" description="进入聊天页，让 AI 基于资料回答问题" />
      </el-steps>

      <div class="quick-actions mt-4">
        <el-button type="primary" @click="$router.push('/knowledge-base')">
          <el-icon><Folder /></el-icon>
          管理知识库
        </el-button>
        <el-button type="success" @click="$router.push('/documents')">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
        <el-button type="warning" @click="$router.push('/chat')">
          <el-icon><ChatDotRound /></el-icon>
          AI 问答
        </el-button>
      </div>
    </el-card>

    <el-card class="mt-4">
      <template #header>
        <div class="card-header">
          <span>当前进展</span>
        </div>
      </template>
      <el-empty
        v-if="knowledgeStore.knowledgeBaseList.length === 0"
        description="还没有知识库，先去创建一个吧"
      />
      <div v-else class="summary-list">
        <div class="summary-item">
          当前知识库：{{ knowledgeStore.currentKb?.name || '未选择' }}
        </div>
        <div class="summary-item">
          已接入知识库：{{ knowledgeStore.knowledgeBaseList.length }} 个
        </div>
        <div class="summary-item">
          当前用户：{{ userStore.username }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Document, ChatDotRound, Folder, Upload } from '@element-plus/icons-vue'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'
import { getConversationList } from '@/api/chat'
import { getDocumentList } from '@/api/document'
import { getCategoryTree } from '@/api/knowledge'

const knowledgeStore = useKnowledgeStore()
const userStore = useUserStore()

const loading = ref(false)
const documentCount = ref(0)
const conversationCount = ref(0)
const categoryCount = ref(0)

const activeStep = computed(() => {
  if (knowledgeStore.knowledgeBaseList.length === 0) return 0
  if (documentCount.value === 0) return 1
  return 2
})

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([knowledgeStore.fetchKnowledgeBaseList(), userStore.hydrateUser().catch(() => null)])

    const kbIds = knowledgeStore.knowledgeBaseList.map((kb) => kb.id)
    const docResults = await Promise.all(
      kbIds.map((kbId) =>
        getDocumentList({ knowledgeBaseId: kbId, page: 1, size: 1 }).catch(() => ({
          list: [],
          total: 0,
          page: 1,
          size: 1
        }))
      )
    )
    documentCount.value = docResults.reduce((sum, item) => sum + item.total, 0)

    const categoryTrees = await Promise.all(
      kbIds.map((kbId) => getCategoryTree(kbId).catch(() => []))
    )
    categoryCount.value = categoryTrees.reduce((sum, tree) => sum + countTreeNodes(tree), 0)

    const currentKbId = knowledgeStore.currentKbId
    if (currentKbId) {
      const conversations = await getConversationList({ knowledgeBaseId: currentKbId, page: 1, size: 100 })
      conversationCount.value = conversations.length
    }
  } finally {
    loading.value = false
  }
})

function countTreeNodes(nodes: Array<{ children?: Array<{ children?: Array<any> }> }>): number {
  return nodes.reduce((sum, node) => sum + 1 + countTreeNodes(node.children || []), 0)
}
</script>

<style scoped>
.dashboard h2 {
  margin-bottom: 20px;
  color: #333;
}

.mt-4 {
  margin-top: 20px;
}

.stat-card {
  text-align: center;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.card-header {
  font-weight: bold;
}

.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.summary-list {
  display: grid;
  gap: 12px;
}

.summary-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f9fc;
  color: #333;
}
</style>
