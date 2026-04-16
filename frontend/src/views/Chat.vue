<template>
  <div class="chat page-container">
    <div class="chat-container">
      <aside class="sidebar">
        <div class="sidebar-header">
          <h3>会话历史</h3>
          <el-button type="primary" size="small" @click="handleNewChat">
            <el-icon><Plus /></el-icon>
            新建
          </el-button>
        </div>
        <el-scrollbar>
          <div class="conversation-list">
            <div
              v-for="item in conversationList"
              :key="item.id"
              class="conversation-item"
              :class="{ active: currentConversationId === item.id }"
              @click="handleSelectConversation(item)"
            >
              <div class="conversation-title">{{ item.title || '未命名会话' }}</div>
              <div class="conversation-meta">{{ item.messageCount }} 条消息</div>
              <el-button text type="danger" size="small" @click.stop="handleDeleteConversation(item)">删除</el-button>
            </div>
            <el-empty v-if="conversationList.length === 0" description="暂无会话" :image-size="70" />
          </div>
        </el-scrollbar>
      </aside>

      <section class="main">
        <div class="main-header">
          <div>
            <div class="chat-title">{{ currentConversationTitle || '新会话' }}</div>
            <div class="chat-subtitle">{{ knowledgeStore.currentKb?.name || '请先选择知识库' }}</div>
          </div>
          <div class="header-actions">
            <el-button v-if="knowledgeStore.currentKbId" @click="handleOpenRetrievalDebug">检索调试</el-button>
            <el-button v-if="currentConversationId" @click="memoryDrawerVisible = true">会话记忆</el-button>
            <el-button v-if="currentConversationId" @click="handleExport('markdown')">导出 Markdown</el-button>
            <el-button v-if="currentConversationId" @click="handleExport('pdf')">导出 PDF</el-button>
          </div>
        </div>

        <div class="message-panel">
          <el-scrollbar ref="scrollbarRef">
            <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
              <el-avatar
                class="message-avatar"
                :src="msg.role === 'user' ? userAvatarPreview || undefined : assistantAvatarPreview || undefined"
                :icon="msg.role === 'user' ? (!userAvatarPreview ? User : undefined) : (!assistantAvatarPreview ? ChatDotRound : undefined)"
              >
                <template v-if="msg.role === 'assistant' && !assistantAvatarPreview">AI</template>
                <template v-else-if="msg.role === 'user' && !userAvatarPreview">{{ userInitial }}</template>
              </el-avatar>

              <div class="message-body">
                <div class="bubble">
                  <div v-html="renderContent(msg.content)" class="message-text"></div>
                </div>

                <div v-if="msg.thinking" class="thinking-box">
                  <button
                    type="button"
                    class="thinking-toggle"
                    :class="{ expanded: isThinkingExpanded(msg.id) }"
                    @click="toggleThinking(msg.id)"
                  >
                    <span>思考过程 {{ formatThinkingMeta(msg.thinking) }}</span>
                    <el-icon><ArrowRight /></el-icon>
                  </button>
                  <pre v-if="isThinkingExpanded(msg.id)" class="thinking-content">{{ msg.thinking }}</pre>
                </div>

                <div v-if="msg.sources?.length" class="sources">
                  <div class="sources-title">引用来源</div>
                  <div v-for="(source, index) in msg.sources" :key="index" class="source-card">
                    <div class="source-head">
                      <el-tag size="small" type="success">{{ source.documentTitle }}</el-tag>
                      <span>{{ Math.round((source.score || 0) * 100) }}%</span>
                    </div>
                    <div class="source-content">{{ source.content }}</div>
                  </div>
                </div>

                <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </div>
          </el-scrollbar>
        </div>

        <div class="input-panel">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="4"
            placeholder="输入你的问题，按 Ctrl+Enter 发送"
            :disabled="sending"
            @keydown.ctrl.enter.prevent="handleSend"
          />
          <div class="input-footer">
            <span class="draft-hint">当前输入会自动保存在本地，切换页面不会丢失。</span>
            <div class="footer-actions">
              <el-button v-if="sending" @click="handleAbort">停止生成</el-button>
              <el-button type="primary" :loading="sending" @click="handleSend">
                <el-icon><Promotion /></el-icon>
                发送
              </el-button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <el-drawer v-model="memoryDrawerVisible" title="会话记忆" size="480px">
      <el-empty v-if="!currentConversationId" description="请先进入一个已有会话" />
      <template v-else>
        <div class="memory-tip">这里保存当前会话的压缩摘要与长期事实，用于长对话续航。</div>
        <el-form label-position="top">
          <el-form-item label="会话摘要">
            <el-input v-model="memoryForm.sessionSummary" type="textarea" :rows="8" maxlength="4000" show-word-limit />
          </el-form-item>
          <el-form-item label="会话事实">
            <el-input v-model="memoryForm.sessionFacts" type="textarea" :rows="8" maxlength="4000" show-word-limit />
          </el-form-item>
        </el-form>
        <div class="drawer-actions">
          <el-button @click="handleClearMemory">清空记忆</el-button>
          <el-button type="primary" :loading="memorySaving" @click="handleSaveMemory">保存记忆</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="retrievalDrawerVisible" title="检索调试" size="720px">
      <div class="debug-toolbar">
        <el-input
          v-model="retrievalForm.query"
          type="textarea"
          :rows="3"
          placeholder="输入一个问题，查看 query rewrite 和向量检索命中结果"
        />
        <div class="debug-toolbar-actions">
          <el-input-number v-model="retrievalForm.topK" :min="1" :max="20" />
          <el-button type="primary" :loading="retrievalLoading" @click="handleRunRetrievalDebug">开始调试</el-button>
        </div>
      </div>

      <template v-if="retrievalDebugData">
        <div class="debug-summary">
          <div><strong>原始问题：</strong>{{ retrievalDebugData.originalQuery }}</div>
          <div><strong>改写问题：</strong>{{ retrievalDebugData.rewrittenQuery || '未触发改写' }}</div>
          <div><strong>最终采用：</strong>{{ retrievalDebugData.usedQuery }}</div>
        </div>

        <div class="debug-section">
          <div class="debug-section-head">
            <h4>最终命中</h4>
            <span>{{ retrievalDebugData.finalHits.length }} 条</span>
          </div>
          <div v-if="retrievalDebugData.finalHits.length" class="debug-hit-list">
            <div v-for="hit in retrievalDebugData.finalHits" :key="`final-${hit.chunkId}`" class="debug-hit">
              <div class="debug-hit-head">
                <strong>{{ hit.documentTitle || '未命名文档' }}</strong>
                <div class="debug-hit-tags">
                  <el-tag size="small" :type="hit.chunkType === 'summary' ? 'success' : 'info'">
                    {{ hit.chunkType || 'unknown' }}
                  </el-tag>
                  <el-tag size="small" effect="plain">chunk {{ hit.chunkIndex }}</el-tag>
                  <span>{{ Math.round((hit.score || 0) * 100) }}%</span>
                </div>
              </div>
              <div class="debug-hit-content">{{ hit.content }}</div>
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
import { ArrowRight, ChatDotRound, Plus, Promotion, User } from '@element-plus/icons-vue'
import { useKnowledgeStore } from '@/stores/knowledge'
import { useUserStore } from '@/stores/user'
import { chat, debugRetrieval, deleteConversation, exportConversation, getConversationDetail, getConversationList, updateConversationMemory } from '@/api/chat'
import type { ChatResponse, Conversation, ConversationDetail, Message } from '@/api/chat'

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
  ElMessageBox.confirm(`确定删除会话“${item.title}”吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteConversation(item.id)
    if (currentConversationId.value === item.id) handleNewChat()
    await loadConversationList()
    ElMessage.success('删除成功')
  })
}

async function handleSend() {
  if (!inputMessage.value.trim()) return
  if (!knowledgeStore.currentKbId) {
    ElMessage.warning('请先选择知识库')
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
      ElMessage.info('已停止生成')
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
    ElMessage.success('会话记忆已保存')
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
    ElMessage.warning('请先选择知识库')
    return
  }
  retrievalDrawerVisible.value = true
  if (!retrievalForm.value.query.trim()) retrievalForm.value.query = getDefaultRetrievalQuery()
  if (retrievalForm.value.query.trim() && !retrievalDebugData.value) await handleRunRetrievalDebug()
}

async function handleRunRetrievalDebug() {
  if (!knowledgeStore.currentKbId) {
    ElMessage.warning('请先选择知识库')
    return
  }
  if (!retrievalForm.value.query.trim()) {
    ElMessage.warning('请输入要调试的问题')
    return
  }
  retrievalLoading.value = true
  try {
    retrievalDebugData.value = await debugRetrieval({
      knowledgeBaseId: knowledgeStore.currentKbId,
      message: retrievalForm.value.query.trim(),
      topK: retrievalForm.value.topK
    })
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
.chat { padding: 0; height: calc(100vh - 60px); }
.chat-container { display: flex; height: 100%; background: linear-gradient(180deg, #fff 0%, #f8fbff 100%); border-radius: 12px; overflow: hidden; }
.sidebar { width: 300px; border-right: 1px solid #e6edf5; display: flex; flex-direction: column; background: rgba(255,255,255,.92); }
.sidebar-header, .main-header { padding: 18px 16px; border-bottom: 1px solid #e6edf5; display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.sidebar-header h3, .debug-section-head h4 { margin: 0; }
.conversation-list { padding: 10px; }
.conversation-item { padding: 12px; border: 1px solid transparent; border-radius: 12px; margin-bottom: 8px; background: #fff; cursor: pointer; }
.conversation-item.active { background: #edf5ff; border-color: #bfdcff; }
.conversation-title { font-size: 14px; color: #223046; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.conversation-meta, .message-time, .draft-hint, .debug-section-head span { font-size: 12px; color: #7b8ba1; }
.main { flex: 1; display: flex; flex-direction: column; }
.chat-title { font-size: 18px; font-weight: 600; color: #1f2a3d; }
.chat-subtitle { margin-top: 4px; font-size: 13px; color: #7b8ba1; }
.header-actions, .footer-actions, .drawer-actions, .debug-toolbar-actions, .debug-hit-tags { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.message-panel { flex: 1; padding: 24px; overflow: auto; }
.message-item { display: flex; gap: 12px; margin-bottom: 22px; }
.message-item.user { flex-direction: row-reverse; }
.message-avatar { flex-shrink: 0; }
.message-body { max-width: min(76%, 900px); }
.message-item.user .message-body { text-align: right; }
.bubble { display: inline-block; padding: 14px 16px; border-radius: 16px; border: 1px solid #e6edf5; background: #fff; box-shadow: 0 12px 32px rgba(27,46,94,.04); }
.message-item.user .bubble { background: linear-gradient(135deg, #409eff, #5cabff); color: #fff; border-color: transparent; }
.message-text { font-size: 14px; line-height: 1.75; word-break: break-word; }
.message-text :deep(p) { margin: 0 0 10px; }
.message-text :deep(p:last-child) { margin-bottom: 0; }
.thinking-box, .sources { margin-top: 10px; }
.thinking-toggle { width: 100%; display: flex; justify-content: space-between; align-items: center; border: 1px solid #d9e7f7; border-radius: 14px; background: #f6faff; padding: 10px 14px; cursor: pointer; color: #35506f; }
.thinking-toggle.expanded :deep(svg) { transform: rotate(90deg); }
.thinking-content, .debug-hit-content, .source-content { white-space: pre-wrap; word-break: break-word; line-height: 1.7; }
.thinking-content { margin: 8px 0 0; padding: 12px; border-radius: 14px; border: 1px solid #d9e7f7; background: #fbfdff; font-size: 12px; color: #48617d; }
.sources-title { font-size: 12px; color: #71839a; margin-bottom: 8px; }
.source-card, .debug-hit, .debug-summary { padding: 12px; border: 1px solid #dce9f8; border-radius: 12px; background: #fff; margin-bottom: 10px; }
.source-head, .debug-hit-head, .debug-section-head { display: flex; justify-content: space-between; gap: 10px; align-items: flex-start; }
.input-panel { border-top: 1px solid #e6edf5; padding: 18px 20px; background: rgba(255,255,255,.86); }
.input-footer { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-top: 12px; }
.memory-tip { margin-bottom: 16px; padding: 12px 14px; border-radius: 12px; background: #f7fbff; color: #637892; line-height: 1.7; }
.debug-toolbar, .debug-section { display: grid; gap: 14px; }
@media (max-width: 960px) { .sidebar { width: 240px; } .message-body { max-width: 85%; } .main-header, .input-footer { flex-direction: column; align-items: stretch; } }
</style>
