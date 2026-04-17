import fs from 'fs'
import path from 'path'
const file = path.resolve('D:/aworkspace/ai-knowledge-base/frontend/src/views/Dashboard.vue')
const content = \<template>
  <div class="p-6 max-w-6xl mx-auto w-full">
    <div class="mb-8">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white">欢迎使用 AI 知识库</h2>
      <p class="text-gray-500 dark:text-gray-400 mt-1">您的智能文档管理和问答助手</p>
    </div>
    <!-- Stats Row -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm flex items-center gap-4 transition-all hover:shadow-md">
        <div class="w-14 h-14 rounded-xl bg-blue-50 dark:bg-blue-900/30 text-blue-500 flex items-center justify-center shrink-0">
          <FileText class="w-7 h-7" />
        </div>
        <div>
          <div class="text-3xl font-bold text-gray-900 dark:text-white" v-if="!loading">{{ documentCount }}</div>
          <div class="text-3xl font-bold text-gray-300 dark:text-gray-600 animate-pulse" v-else>-</div>
          <div class="text-sm font-medium text-gray-500 dark:text-gray-400 mt-1">文档总数</div>
        </div>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm flex items-center gap-4 transition-all hover:shadow-md">
        <div class="w-14 h-14 rounded-xl bg-green-50 dark:bg-green-900/30 text-green-500 flex items-center justify-center shrink-0">
          <MessageSquare class="w-7 h-7" />
        </div>
        <div>
          <div class="text-3xl font-bold text-gray-900 dark:text-white" v-if="!loading">{{ conversationCount }}</div>
          <div class="text-3xl font-bold text-gray-300 dark:text-gray-600 animate-pulse" v-else>-</div>
          <div class="text-sm font-medium text-gray-500 dark:text-gray-400 mt-1">会话数量</div>
        </div>
      </div>
      <div class="bg-white dark:bg-gray-800 rounded-2xl p-6 border border-gray-100 dark:border-gray-700 shadow-sm flex items-center gap-4 transition-all hover:shadow-md">
        <div class="w-14 h-14 rounded-xl bg-amber-50 dark:bg-amber-900/30 text-amber-500 flex items-center justify-center shrink-0">
          <FolderTree class="w-7 h-7" />
        </div>
        <div>
          <div class="text-3xl font-bold text-gray-900 dark:text-white" v-if="!loading">{{ categoryCount }}</div>
          <div class="text-3xl font-bold text-gray-300 dark:text-gray-600 animate-pulse" v-else>-</div>
          <div class="text-sm font-medium text-gray-500 dark:text-gray-400 mt-1">分类数量</div>
        </div>
      </div>
    </div>
    <!-- Quick Start -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl p-8 border border-gray-100 dark:border-gray-700 shadow-sm mb-8">
      <h3 class="text-lg font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
        <Zap class="w-5 h-5 text-yellow-500" />
        快速开始
      </h3>
      <div class="mb-8">
        <el-steps :active="activeStep" align-center>
          <el-step title="创建知识库" description="先创建一个知识库容器" />
          <el-step title="上传资料" description="把 PDF、Markdown、Word 等资料放进去" />
          <el-step title="开始问答" description="进入聊天页，让 AI 基于资料回答问题" />
        </el-steps>
      </div>
      <div class="flex flex-wrap gap-4 justify-center">
        <el-button type="primary" size="large" @click=".push('/knowledge-base')" class="!rounded-xl !px-6">
          <template #icon><Database class="w-4 h-4" /></template>
          管理知识库
        </el-button>
        <el-button type="success" size="large" @click=".push('/documents')" class="!rounded-xl !px-6">
          <template #icon><Upload class="w-4 h-4" /></template>
          上传文档
        </el-button>
        <el-button type="warning" size="large" @click=".push('/chat')" class="!rounded-xl !px-6">
          <template #icon><Bot class="w-4 h-4" /></template>
          AI 问答
        </el-button>
      </div>
    </div>
    <!-- Current Progress -->
    <div class="bg-white dark:bg-gray-800 rounded-2xl p-8 border border-gray-100 dark:border-gray-700 shadow-sm">
       <h3 class="text-lg font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
        <Activity class="w-5 h-5 text-blue-500" />
        当前状态
      </h3>
      <el-empty
        v-if="knowledgeStore.knowledgeBaseList.length === 0"
        description="还没有知识库，先去创建一个吧"
      />
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400 flex items-center justify-center">
            <Database class="w-5 h-5" />
          </div>
          <div>
            <div class="text-sm text-gray-500 dark:text-gray-400">当前知识库</div>
            <div class="font-medium text-gray-900 dark:text-white">{{ knowledgeStore.currentKb?.name || '未选择' }}</div>
          </div>
        </div>
        <div class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-green-100 text-green-600 dark:bg-green-900/40 dark:text-green-400 flex items-center justify-center">
            <Layers class="w-5 h-5" />
          </div>
          <div>
            <div class="text-sm text-gray-500 dark:text-gray-400">已接入知识库</div>
            <div class="font-medium text-gray-900 dark:text-white">{{ knowledgeStore.knowledgeBaseList.length }} 个</div>
          </div>
        </div>
        <div class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4 flex items-center gap-3">
           <div class="w-10 h-10 rounded-lg bg-purple-100 text-purple-600 dark:bg-purple-900/40 dark:text-purple-400 flex items-center justify-center">
            <UserCircle class="w-5 h-5" />
          </div>
          <div>
            <div class="text-sm text-gray-500 dark:text-gray-400">当前用户</div>
            <div class="font-medium text-gray-900 dark:text-white">{{ userStore.username }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { FileText, MessageSquare, FolderTree, Zap, Database, Upload, Bot, Activity, Layers, UserCircle } from 'lucide-vue-next'
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
function countTreeNodes(nodes: Array<{ children?: Array<any> }>): number {
  return nodes.reduce((sum, node) => sum + 1 + countTreeNodes(node.children || []), 0)
}
</script>
\
fs.writeFileSync(file, content, 'utf-8')
