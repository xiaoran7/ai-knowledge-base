<template>
  <div class="h-[calc(100vh-60px)] w-full flex bg-gray-50 dark:bg-gray-900 transition-colors duration-300 overflow-hidden">
    <div class="w-full h-full flex m-2 bg-white/80 dark:bg-gray-800/80 backdrop-blur-md shadow-xl rounded-2xl border border-gray-100 dark:border-gray-700 overflow-hidden">
      <!-- Sidebar -->
      <aside class="w-72 border-r border-gray-100 dark:border-gray-700 flex flex-col bg-white/50 dark:bg-gray-800/50">
        <div class="p-4 border-b border-gray-100 dark:border-gray-700 flex justify-between items-center">
          <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-300">会话历史</h3>
          <el-button type="primary" size="small" @click="handleNewChat" class="!rounded-lg">
            <template #icon><Plus class="w-4 h-4" /></template>
            新建
          </el-button>
        </div>
        <el-scrollbar class="flex-1">
          <div class="p-3 space-y-2">
            <div
              v-for="item in conversationList"
              :key="item.id"
              class="p-3 rounded-xl border cursor-pointer transition-all duration-200 group relative"
              :class="currentConversationId === item.id ? 'bg-blue-50 border-blue-200 dark:bg-blue-900/30 dark:border-blue-800/50' : 'bg-white border-transparent hover:border-gray-200 dark:bg-gray-800/50 dark:hover:border-gray-700'"
              @click="handleSelectConversation(item)"
            >
              <div class="text-sm font-medium text-gray-900 dark:text-white truncate pr-12">{{ item.title || '未命名会话' }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400 mt-1">{{ item.messageCount }} 条消息</div>
              <el-button
                text
                type="danger"
                size="small"
                class="!absolute right-2 top-2 opacity-0 group-hover:opacity-100 transition-opacity"
                @click.stop="handleDeleteConversation(item)"
              >
                删除
              </el-button>
            </div>
            <el-empty v-if="conversationList.length === 0" description="暂无会话" :image-size="70" />
          </div>
        </el-scrollbar>
      </aside>

      <!-- Main Chat Area -->
      <section class="flex-1 flex flex-col min-w-0">
        <!-- Header -->
        <div class="p-4 border-b border-gray-100 dark:border-gray-700 flex justify-between items-center bg-white/50 dark:bg-gray-800/50">
          <div>
            <div class="text-lg font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <MessageSquare class="w-5 h-5 text-blue-500" />
              {{ currentConversationTitle || '新会话' }}
            </div>
            <div class="text-xs text-gray-500 dark:text-gray-400 mt-1 flex items-center gap-1">
              <Database class="w-3 h-3" />
              {{ knowledgeStore.currentKb?.name || '请先选择知识库' }}
            </div>
          </div>
          <div class="flex gap-2">
            <el-button v-if="knowledgeStore.currentKbId" size="small" @click="handleOpenRetrievalDebug" plain>检索调试</el-button>
            <el-button v-if="currentConversationId" size="small" @click="memoryDrawerVisible = true" plain>会话记忆</el-button>
            <el-button v-if="currentConversationId" size="small" @click="handleExport('markdown')" plain>导出 Markdown</el-button>
            <el-button v-if="currentConversationId" size="small" @click="handleExport('pdf')" plain>导出 PDF</el-button>
          </div>
        </div>

        <!-- Messages -->
        <div class="flex-1 p-4 overflow-hidden">
          <el-scrollbar ref="scrollbarRef">
            <div class="max-w-4xl mx-auto space-y-6 pb-4">
              <div v-for="msg in messages" :key="msg.id" class="flex gap-4" :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
                <!-- Avatar -->
                <el-avatar
                  class="shrink-0 shadow-sm"
                  :src="msg.role === 'user' ? userAvatarPreview || undefined : assistantAvatarPreview || undefined"
                  :icon="msg.role === 'user' ? (!userAvatarPreview ? User : undefined) : (!assistantAvatarPreview ? Bot : undefined)"
                >
                  <template v-if="msg.role === 'assistant' && !assistantAvatarPreview">AI</template>
                  <template v-else-if="msg.role === 'user' && !userAvatarPreview">{{ userInitial }}</template>
                </el-avatar>

                <!-- Message Body -->
                <div class="flex flex-col max-w-[80%]" :class="msg.role === 'user' ? 'items-end' : 'items-start'">
                  <div
                    class="px-5 py-3.5 rounded-2xl shadow-sm text-sm"
                    :class="msg.role === 'user' ? 'bg-blue-600 text-white rounded-tr-sm' : 'bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 text-gray-800 dark:text-gray-200 rounded-tl-sm'"
                  >
                    <div v-html="renderContent(msg.content)" class="prose dark:prose-invert max-w-none break-words"></div>
                  </div>

                  <div v-if="msg.toolCalls?.length" class="mt-2 w-full space-y-2">
                    <div class="text-xs text-gray-500 flex items-center gap-1.5">
                      <Database class="w-3.5 h-3.5" />
                      已执行工具
                    </div>
                    <div
                      v-for="tool in msg.toolCalls"
                      :key="`${msg.id}-${tool.name}-${tool.title}`"
                      class="rounded-2xl border px-4 py-3"
                      :class="getToolCardClass(tool.status)"
                    >
                      <div class="flex items-start justify-between gap-3">
                        <div>
                          <div class="text-sm font-semibold text-gray-900 dark:text-white">{{ tool.title }}</div>
                          <div class="mt-1 text-xs text-gray-600 dark:text-gray-300">{{ tool.summary }}</div>
                        </div>
                        <el-tag
                          size="small"
                          :type="getToolTagType(tool.status)"
                          class="!rounded-full !px-3 shrink-0"
                        >
                          {{ getToolStatusText(tool.status) }}
                        </el-tag>
                      </div>
                      <pre
                        v-if="tool.detail"
                        class="mt-2 whitespace-pre-wrap rounded-xl bg-white/70 px-3 py-2 text-xs text-gray-600 dark:bg-gray-900/30 dark:text-gray-300"
                      >{{ tool.detail }}</pre>
                    </div>
                  </div>

                  <!-- Thinking Box -->
                  <div v-if="msg.thinking" class="mt-2 w-full max-w-full">
                    <button
                      type="button"
                      class="flex w-full items-center justify-between px-3 py-2 text-xs rounded-xl border border-blue-100 bg-blue-50/50 text-blue-700 hover:bg-blue-50 dark:border-blue-900/30 dark:bg-blue-900/10 dark:text-blue-400 transition-colors"
                      @click="toggleThinking(msg.id)"
                    >
                      <span class="flex items-center gap-1.5"><Brain class="w-3.5 h-3.5" /> 思考过程 {{ formatThinkingMeta(msg.thinking) }}</span>
                      <ChevronRight class="w-3.5 h-3.5 transition-transform duration-200" :class="{ 'rotate-90': isThinkingExpanded(msg.id) }" />
                    </button>
                    <pre v-if="isThinkingExpanded(msg.id)" class="mt-1.5 p-3 rounded-xl border border-gray-100 bg-gray-50 text-xs text-gray-600 dark:border-gray-700 dark:bg-gray-800/50 dark:text-gray-400 whitespace-pre-wrap font-mono">{{ msg.thinking }}</pre>
                  </div>

                  <!-- Sources -->
                  <div v-if="msg.sources?.length" class="mt-2 w-full">
                    <div class="text-xs text-gray-500 mb-1.5 flex items-center gap-1.5"><FileText class="w-3.5 h-3.5" /> 引用来源</div>
                    <div v-for="(source, index) in msg.sources" :key="index" class="p-3 mb-2 rounded-xl border border-green-100 bg-green-50/30 dark:border-green-900/30 dark:bg-green-900/10">
                      <div class="flex justify-between items-start gap-2 mb-1.5">
                        <el-tag size="small" type="success" class="!rounded-md max-w-full truncate">{{ source.documentTitle }}</el-tag>
                        <span class="text-xs font-mono text-green-600 dark:text-green-400 shrink-0">{{ Math.round((source.score || 0) * 100) }}%</span>
                      </div>
                      <div class="text-xs text-gray-600 dark:text-gray-400 line-clamp-3 hover:line-clamp-none transition-all">{{ source.content }}</div>
                    </div>
                  </div>

                  <div class="text-[11px] text-gray-400 mt-1.5">{{ formatTime(msg.createdAt) }}</div>
                </div>
              </div>
            </div>
          </el-scrollbar>
        </div>

        <!-- Input Panel -->
        <div class="p-4 border-t border-gray-100 dark:border-gray-700 bg-white/80 dark:bg-gray-800/80">
          <div class="max-w-4xl mx-auto">
            <div class="relative rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm focus-within:ring-2 focus-within:ring-blue-500/20 focus-within:border-blue-500 transition-all">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="4"
                placeholder="输入你的问题，按 Ctrl+Enter 发送"
                :disabled="sending"
                class="!border-none"
                input-style="box-shadow: none; background: transparent; padding: 12px 16px; border-radius: 12px;"
                @keydown.ctrl.enter.prevent="handleSend"
              />
              <div class="absolute bottom-3 right-3 flex items-center gap-2">
                <span class="text-xs text-gray-400 hidden sm:inline-block">Ctrl + Enter</span>
                <el-button v-if="sending" @click="handleAbort" type="danger" plain size="small" class="!rounded-lg">
                  <template #icon><Square class="w-4 h-4" /></template>
                  停止
                </el-button>
                <el-button type="primary" :loading="sending" @click="handleSend" size="small" class="!rounded-lg">
                  <template #icon><Send class="w-4 h-4" /></template>
                  发送
                </el-button>
              </div>
            </div>
            <div class="mt-2 text-center text-xs text-gray-400">当前输入会自动保存在本地，切换页面不会丢失。</div>
          </div>
        </div>
      </section>
    </div>

    <!-- Drawers remain standard Element Plus for now, but apply Tailwind utility classes where easy -->
    <el-drawer v-model="memoryDrawerVisible" title="会话记忆" size="480px">
      <el-empty v-if="!currentConversationId" description="请先进入一个已有会话" />
      <template v-else>
        <div class="mb-4 p-3 rounded-xl bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400 text-sm flex items-start gap-2">
           <Info class="w-4 h-4 mt-0.5 shrink-0" />
           这里保存当前会话的压缩摘要与长期事实，用于长对话续航。
        </div>
        <el-form label-position="top">
          <el-form-item label="会话摘要">
            <el-input v-model="memoryForm.sessionSummary" type="textarea" :rows="8" maxlength="4000" show-word-limit />
          </el-form-item>
          <el-form-item label="会话事实">
            <el-input v-model="memoryForm.sessionFacts" type="textarea" :rows="8" maxlength="4000" show-word-limit />
          </el-form-item>
        </el-form>
        <div class="flex gap-3 justify-end mt-6">
          <el-button @click="handleClearMemory">清空记忆</el-button>
          <el-button type="primary" :loading="memorySaving" @click="handleSaveMemory">保存记忆</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="retrievalDrawerVisible" title="检索调试" size="720px">
      <div class="space-y-4">
        <el-input
          v-model="retrievalForm.query"
          type="textarea"
          :rows="3"
          placeholder="输入一个问题，查看 query rewrite 和向量检索命中结果"
        />
        <div class="flex items-center gap-3 justify-end">
          <span class="text-sm text-gray-500">Top K:</span>
          <el-input-number v-model="retrievalForm.topK" :min="1" :max="20" size="small" />
          <el-button type="primary" :loading="retrievalLoading" @click="handleRunRetrievalDebug">开始调试</el-button>
        </div>
      </div>

      <template v-if="retrievalDebugData">
        <div class="mt-6 p-4 rounded-xl border border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-800/50 space-y-2 text-sm">
          <div><strong class="text-gray-900 dark:text-gray-100">原始问题：</strong><span class="text-gray-600 dark:text-gray-400">{{ retrievalDebugData.originalQuery }}</span></div>
          <div><strong class="text-gray-900 dark:text-gray-100">改写问题：</strong><span class="text-gray-600 dark:text-gray-400">{{ retrievalDebugData.rewrittenQuery || '未触发改写' }}</span></div>
          <div><strong class="text-gray-900 dark:text-gray-100">最终采用：</strong><span class="text-gray-600 dark:text-gray-400">{{ retrievalDebugData.usedQuery }}</span></div>
        </div>

        <div class="mt-6">
          <div class="flex justify-between items-center mb-4">
            <h4 class="font-medium text-gray-900 dark:text-gray-100">最终命中</h4>
            <span class="text-sm text-gray-500 bg-gray-100 dark:bg-gray-800 px-2 py-0.5 rounded-full">{{ retrievalDebugData.finalHits.length }} 条</span>
          </div>
          <div v-if="retrievalDebugData.finalHits.length" class="space-y-3">
            <div v-for="hit in retrievalDebugData.finalHits" :key="`final-${hit.chunkId}`" class="p-4 rounded-xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800">
              <div class="flex justify-between items-start gap-3 mb-2">
                <strong class="text-sm text-gray-900 dark:text-white">{{ hit.documentTitle || '未命名文档' }}</strong>
                <div class="flex items-center gap-2 shrink-0">
                  <el-tag size="small" :type="hit.chunkType === 'summary' ? 'success' : 'info'" class="!rounded-md">
                    {{ hit.chunkType || 'unknown' }}
                  </el-tag>
                  <el-tag size="small" effect="plain" class="!rounded-md">chunk {{ hit.chunkIndex }}</el-tag>
                  <span class="text-xs font-mono text-gray-500">{{ Math.round((hit.score || 0) * 100) }}%</span>
                </div>
              </div>
              <div class="text-sm text-gray-600 dark:text-gray-300 whitespace-pre-wrap">{{ hit.content }}</div>
            </div>
          </div>
          <el-empty v-else description="当前问题没有命中检索结果" :image-size="60" />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script lang="ts">
import { reactive as sharedReactive } from 'vue'
import type { Conversation as ConversationItem, Message as ChatMessage, RetrievalDebugResponse } from '@/api/chat'

interface ChatRuntimeState {
  conversationList: ConversationItem[]
  currentConversationId: string
  currentConversationTitle: string
  messages: ChatMessage[]
  inputMessage: string
  sending: boolean
  expandedThinkingIds: string[]
  abortController: AbortController | null
}

const chatRuntimeState = sharedReactive<ChatRuntimeState>({
  conversationList: [],
  currentConversationId: '',
  currentConversationTitle: '',
  messages: [],
  inputMessage: '',
  sending: false,
  expandedThinkingIds: [],
  abortController: null
})

const retrievalDebugState = sharedReactive<{
  form: { query: string; topK: number }
  data: RetrievalDebugResponse | null
}>({
  form: { query: '', topK: 8 },
  data: null
})
</script>

<script setup lang="ts">
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, toRef, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Brain, ChevronRight, Database, FileText, Info, MessageSquare, Plus, Send, Square, Bot, User } from 'lucide-vue-next'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'
import { chat, debugRetrieval, deleteConversation, exportConversation, getConversationDetail, getConversationList, updateConversationMemory } from '@/api/chat'
import type { ChatResponse, Conversation, ConversationDetail, Message, ToolStatus } from '@/api/chat'

const CHAT_CACHE_KEY = 'chat_page_state_v1'

interface ChatCacheState {
  knowledgeBaseId: string
  currentConversationId: string
  inputMessage: string
  messages: Message[]
}

const knowledgeStore = useKnowledgeStore()
const userStore = useUserStore()
const md = new MarkdownIt({ breaks: true, linkify: true })

const conversationList = toRef(chatRuntimeState, 'conversationList')
const currentConversationId = toRef(chatRuntimeState, 'currentConversationId')
const currentConversationTitle = toRef(chatRuntimeState, 'currentConversationTitle')
const messages = toRef(chatRuntimeState, 'messages')
const inputMessage = toRef(chatRuntimeState, 'inputMessage')
const sending = toRef(chatRuntimeState, 'sending')
const abortController = toRef(chatRuntimeState, 'abortController')
const expandedThinkingIds = toRef(chatRuntimeState, 'expandedThinkingIds')
const retrievalForm = toRef(retrievalDebugState, 'form')
const retrievalDebugData = toRef(retrievalDebugState, 'data')

const scrollbarRef = ref<{ setScrollTop: (value: number) => void } | null>(null)
const memoryDrawerVisible = ref(false)
const memorySaving = ref(false)
const retrievalDrawerVisible = ref(false)
const retrievalLoading = ref(false)
const memoryForm = reactive({ sessionSummary: '', sessionFacts: '' })

const userInitial = computed(() => (userStore.username || 'U').charAt(0).toUpperCase())

function resolveAssetUrl(url?: string) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return `/api${url}`
}

const userAvatarPreview = computed(() => resolveAssetUrl(userStore.user?.avatarUrl))
const assistantAvatarPreview = computed(() => resolveAssetUrl(userStore.user?.assistantAvatarUrl))

const renderContent = (content: string) => md.render(content || '')
const formatTime = (time: string) => new Date(time).toLocaleString('zh-CN')
const isThinkingExpanded = (messageId: string) => expandedThinkingIds.value.includes(messageId)
const formatThinkingMeta = (thinking?: string) => {
  const lines = (thinking || '').split('\n').map((line) => line.trim()).filter(Boolean).length
  return lines ? `${lines} 行` : '点击展开'
}

function getToolCardClass(status?: ToolStatus) {
  if (status === 'success') {
    return 'border-sky-100 bg-sky-50/70 dark:border-sky-900/30 dark:bg-sky-900/10'
  }
  if (status === 'blocked') {
    return 'border-amber-100 bg-amber-50/80 dark:border-amber-900/30 dark:bg-amber-900/10'
  }
  return 'border-rose-100 bg-rose-50/70 dark:border-rose-900/30 dark:bg-rose-900/10'
}

function getToolTagType(status?: ToolStatus) {
  if (status === 'success') return 'success'
  if (status === 'blocked') return 'warning'
  return 'danger'
}

function getToolStatusText(status?: ToolStatus) {
  if (status === 'success') return '成功'
  if (status === 'blocked') return '待确认'
  return '失败'
}

function toggleThinking(messageId: string) {
  expandedThinkingIds.value = isThinkingExpanded(messageId)
    ? expandedThinkingIds.value.filter((id) => id !== messageId)
    : [...expandedThinkingIds.value, messageId]
}

function persistChatState() {
  const kbId = knowledgeStore.currentKbId
  if (!kbId) {
    localStorage.removeItem(CHAT_CACHE_KEY)
    return
  }
  const payload: ChatCacheState = {
    knowledgeBaseId: kbId,
    currentConversationId: currentConversationId.value,
    inputMessage: inputMessage.value,
    messages: messages.value
  }
  localStorage.setItem(CHAT_CACHE_KEY, JSON.stringify(payload))
}

function restoreChatState() {
  const raw = localStorage.getItem(CHAT_CACHE_KEY)
  if (!raw || !knowledgeStore.currentKbId) return
  try {
    const state = JSON.parse(raw) as ChatCacheState
    if (state.knowledgeBaseId !== knowledgeStore.currentKbId) return
    currentConversationId.value = state.currentConversationId || ''
    inputMessage.value = state.inputMessage || ''
    messages.value = Array.isArray(state.messages) ? state.messages : []
    const matched = conversationList.value.find((item) => item.id === currentConversationId.value)
    currentConversationTitle.value = matched?.title || currentConversationTitle.value
  } catch {
    localStorage.removeItem(CHAT_CACHE_KEY)
  }
}

const syncMemory = (detail: ConversationDetail) => {
  memoryForm.sessionSummary = detail.sessionSummary || ''
  memoryForm.sessionFacts = detail.sessionFacts || ''
}

async function loadConversationList() {
  if (!knowledgeStore.currentKbId) {
    conversationList.value = []
    return
  }
  conversationList.value = await getConversationList({ knowledgeBaseId: knowledgeStore.currentKbId, page: 1, size: 30 })
  const matched = conversationList.value.find((item) => item.id === currentConversationId.value)
  currentConversationTitle.value = matched?.title || currentConversationTitle.value
}

async function scrollToBottom() {
  await nextTick()
  scrollbarRef.value?.setScrollTop(999999)
}

function updateConversationTitleFromResponse(res: ChatResponse) {
  if (res.title) currentConversationTitle.value = res.title
  const matched = conversationList.value.find((item) => item.id === res.conversationId)
  if (matched && res.title) matched.title = res.title
}

function handleNewChat() {
  currentConversationId.value = ''
  currentConversationTitle.value = ''
  messages.value = []
  expandedThinkingIds.value = []
  memoryForm.sessionSummary = ''
  memoryForm.sessionFacts = ''
  persistChatState()
}

async function handleSelectConversation(item: Conversation) {
  currentConversationId.value = item.id
  currentConversationTitle.value = item.title
  const detail = await getConversationDetail(item.id)
  messages.value = detail.messages
  expandedThinkingIds.value = []
  syncMemory(detail)
  if (detail.title) currentConversationTitle.value = detail.title
  persistChatState()
  await scrollToBottom()
}

function handleDeleteConversation(item: Conversation) {
  ElMessageBox.confirm(`确认删除会话“${item.title}”？`, '提示', { type: 'warning' }).then(async () => {
    await deleteConversation(item.id)
    if (currentConversationId.value === item.id) handleNewChat()
    await loadConversationList()
    ElMessage.success('删除成功')
  })
}

async function handleSend() {
  if (!inputMessage.value.trim()) return
  if (!knowledgeStore.currentKbId) {
    ElMessage.warning('请先选择知识库进行提问')
    return
  }

  const question = inputMessage.value.trim()
  const userMessage: Message = { id: `local-${Date.now()}`, role: 'user', content: question, createdAt: new Date().toISOString() }
  messages.value.push(userMessage)
  inputMessage.value = ''
  persistChatState()
  await scrollToBottom()

  sending.value = true
  abortController.value = new AbortController()

  try {
    const res = await chat(
      { knowledgeBaseId: knowledgeStore.currentKbId, message: question, conversationId: currentConversationId.value || undefined },
      { signal: abortController.value.signal }
    )

    if (!currentConversationId.value) currentConversationId.value = res.conversationId

    messages.value.push({
      id: res.messageId,
      role: 'assistant',
      content: res.content,
      thinking: res.thinking,
      sources: res.sources,
      toolCalls: res.toolCalls,
      createdAt: new Date().toISOString()
    })

    if (currentConversationId.value) {
      const detail = await getConversationDetail(currentConversationId.value)
      syncMemory(detail)
    }

    await loadConversationList()
    updateConversationTitleFromResponse(res)
    persistChatState()
  } catch (error) {
    if (axios.isCancel(error) || (error as { code?: string })?.code === 'ERR_CANCELED') {
      ElMessage.info('请求已取消')
    } else {
      messages.value = messages.value.filter((item) => item.id !== userMessage.id)
      persistChatState()
    }
  } finally {
    sending.value = false
    abortController.value = null
    persistChatState()
    await scrollToBottom()
  }
}

const handleAbort = () => abortController.value?.abort()

async function handleExport(format: 'markdown' | 'pdf') {
  if (!currentConversationId.value) return
  const blob = await exportConversation(currentConversationId.value, format)
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${currentConversationTitle.value || 'conversation'}.${format === 'pdf' ? 'pdf' : 'md'}`
  link.click()
  window.URL.revokeObjectURL(url)
}

async function handleSaveMemory() {
  if (!currentConversationId.value) return
  memorySaving.value = true
  try {
    const detail = await updateConversationMemory(currentConversationId.value, {
      sessionSummary: memoryForm.sessionSummary,
      sessionFacts: memoryForm.sessionFacts
    })
    syncMemory(detail)
    ElMessage.success('记忆保存成功')
  } finally {
    memorySaving.value = false
  }
}

async function handleClearMemory() {
  memoryForm.sessionSummary = ''
  memoryForm.sessionFacts = ''
  await handleSaveMemory()
}

function getDefaultRetrievalQuery() {
  if (inputMessage.value.trim()) return inputMessage.value.trim()
  return [...messages.value].reverse().find((item) => item.role === 'user')?.content?.trim() || ''
}

async function handleOpenRetrievalDebug() {
  if (!knowledgeStore.currentKbId) {
    ElMessage.warning('请先选择知识库进行检索调试')
    return
  }
  retrievalDrawerVisible.value = true
  if (!retrievalForm.value.query.trim()) retrievalForm.value.query = getDefaultRetrievalQuery()
  if (retrievalForm.value.query.trim() && !retrievalDebugData.value) await handleRunRetrievalDebug()
}

async function handleRunRetrievalDebug() {
  if (!knowledgeStore.currentKbId) {
    ElMessage.warning('请先选择知识库进行检索调试')
    return
  }
  if (!retrievalForm.value.query.trim()) {
    ElMessage.warning('请输入问题进行检索调试')
    return
  }
  retrievalLoading.value = true
  try {
    retrievalDebugData.value = await debugRetrieval({
      knowledgeBaseId: knowledgeStore.currentKbId,
      message: retrievalForm.value.query.trim(),
      topK: retrievalForm.value.topK
    })
  } catch (error) {
    const message =
      typeof error === 'object' && error && 'message' in error && typeof error.message === 'string'
        ? error.message
        : '检索调试失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    retrievalLoading.value = false
  }
}

watch(() => knowledgeStore.currentKbId, async (newKbId, oldKbId) => {
  if (!newKbId) {
    conversationList.value = []
    retrievalDebugData.value = null
    retrievalForm.value.query = ''
    handleNewChat()
    localStorage.removeItem(CHAT_CACHE_KEY)
    return
  }
  if (oldKbId && oldKbId !== newKbId) {
    retrievalDebugData.value = null
    retrievalForm.value.query = ''
    handleNewChat()
    localStorage.removeItem(CHAT_CACHE_KEY)
  }
  await loadConversationList()
  restoreChatState()
  if (currentConversationId.value) {
    const detail = await getConversationDetail(currentConversationId.value)
    syncMemory(detail)
  }
  await scrollToBottom()
})

watch([inputMessage, currentConversationId, messages], persistChatState, { deep: true })

onMounted(async () => {
  await Promise.all([knowledgeStore.fetchKnowledgeBaseList(), userStore.hydrateUser().catch(() => null)])
  await loadConversationList()
  restoreChatState()
  if (currentConversationId.value) {
    const detail = await getConversationDetail(currentConversationId.value)
    syncMemory(detail)
  }
  await scrollToBottom()
})

onBeforeUnmount(persistChatState)

const selectedConversation = computed(() => conversationList.value.find((item) => item.id === currentConversationId.value) || null)
watch(selectedConversation, (value) => {
  if (value?.title) currentConversationTitle.value = value.title
}, { immediate: true })
</script>

<style scoped>
/* Scoped styles completely removed as we are using Tailwind CSS entirely */
</style>
