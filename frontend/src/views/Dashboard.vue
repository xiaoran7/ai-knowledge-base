<template>
  <div class="mx-auto flex w-full max-w-7xl flex-col gap-8 px-6 py-8 text-slate-900">
    <section class="grid gap-6 lg:grid-cols-[1.4fr_0.95fr]">
      <div class="shell-card shell-hero overflow-hidden">
        <div class="shell-hero__glow shell-hero__glow--blue"></div>
        <div class="shell-hero__glow shell-hero__glow--gold"></div>
        <div class="relative z-10 space-y-6">
          <div class="space-y-3">
            <span class="shell-tag">
              <Sparkles class="h-4 w-4" />
              Workspace pulse
            </span>
            <div class="space-y-3">
              <h1 class="dashboard-hero-title text-slate-950">
                把资料、检索和问答，收进同一套可持续工作的知识流里。
              </h1>
              <p class="max-w-2xl text-sm leading-7 text-slate-600 md:text-[15px]">
                这里是你的 AI 知识库总览。上传文档、维护知识库、进入对话和查看沉淀状态，都可以从这一页快速继续。
              </p>
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-3">
            <div class="shell-inline-metric">
              <span class="shell-inline-metric__label">当前用户</span>
              <strong class="shell-inline-metric__value">{{ userStore.username || '未登录' }}</strong>
            </div>
            <div class="shell-inline-metric">
              <span class="shell-inline-metric__label">当前知识库</span>
              <strong class="shell-inline-metric__value">{{ knowledgeStore.currentKb?.name || '尚未选择' }}</strong>
            </div>
            <div class="shell-inline-metric">
              <span class="shell-inline-metric__label">上手阶段</span>
              <strong class="shell-inline-metric__value">{{ progressLabel }}</strong>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button class="shell-primary-btn" @click="$router.push('/chat')">
              <Bot class="h-4 w-4" />
              继续 AI 问答
            </button>
            <button class="shell-secondary-btn" @click="$router.push('/documents')">
              <Upload class="h-4 w-4" />
              上传文档
            </button>
            <button class="shell-secondary-btn" @click="$router.push('/knowledge-base')">
              <Database class="h-4 w-4" />
              管理知识库
            </button>
          </div>
        </div>
      </div>

      <div class="shell-card space-y-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="shell-eyebrow">Workspace status</p>
            <h2 class="text-xl font-semibold text-slate-950">当前状态</h2>
          </div>
          <div class="shell-icon-badge shell-icon-badge--gold">
            <Activity class="h-5 w-5" />
          </div>
        </div>

        <div class="space-y-4">
          <div class="shell-status-line">
            <span class="shell-status-line__label">知识库接入</span>
            <span class="shell-status-line__value">{{ knowledgeStore.knowledgeBaseList.length }} 个</span>
          </div>
          <div class="shell-status-line">
            <span class="shell-status-line__label">当前对话上下文</span>
            <span class="shell-status-line__value">{{ conversationCount }} 条会话</span>
          </div>
          <div class="shell-status-line">
            <span class="shell-status-line__label">资料覆盖情况</span>
            <span class="shell-status-line__value">{{ documentCount > 0 ? '已有知识资产' : '等待上传' }}</span>
          </div>
        </div>

        <div class="shell-divider"></div>

        <div class="space-y-3">
          <div class="flex items-center justify-between text-sm text-slate-500">
            <span>产品可用性推进度</span>
            <span>{{ activeStep + 1 }}/3</span>
          </div>
          <div class="shell-progress">
            <div class="shell-progress__bar" :style="{ width: `${((activeStep + 1) / 3) * 100}%` }"></div>
          </div>
          <p class="text-sm leading-6 text-slate-600">
            {{ progressDescription }}
          </p>
        </div>
      </div>
    </section>

    <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article class="shell-stat-card">
        <div class="shell-stat-card__icon shell-stat-card__icon--blue">
          <FileText class="h-5 w-5" />
        </div>
        <div class="space-y-2">
          <p class="shell-eyebrow">Documents</p>
          <div class="text-3xl font-semibold text-slate-950">{{ loading ? '-' : documentCount }}</div>
          <p class="text-sm text-slate-500">已进入工作区的知识资料总数</p>
        </div>
      </article>

      <article class="shell-stat-card">
        <div class="shell-stat-card__icon shell-stat-card__icon--green">
          <MessageSquare class="h-5 w-5" />
        </div>
        <div class="space-y-2">
          <p class="shell-eyebrow">Conversations</p>
          <div class="text-3xl font-semibold text-slate-950">{{ loading ? '-' : conversationCount }}</div>
          <p class="text-sm text-slate-500">当前知识库下累计的对话会话</p>
        </div>
      </article>

      <article class="shell-stat-card">
        <div class="shell-stat-card__icon shell-stat-card__icon--amber">
          <FolderTree class="h-5 w-5" />
        </div>
        <div class="space-y-2">
          <p class="shell-eyebrow">Categories</p>
          <div class="text-3xl font-semibold text-slate-950">{{ loading ? '-' : categoryCount }}</div>
          <p class="text-sm text-slate-500">知识分类树中已维护的节点数量</p>
        </div>
      </article>

      <article class="shell-stat-card">
        <div class="shell-stat-card__icon shell-stat-card__icon--slate">
          <Layers class="h-5 w-5" />
        </div>
        <div class="space-y-2">
          <p class="shell-eyebrow">Knowledge Bases</p>
          <div class="text-3xl font-semibold text-slate-950">{{ loading ? '-' : knowledgeStore.knowledgeBaseList.length }}</div>
          <p class="text-sm text-slate-500">用于隔离资料和对话上下文的空间</p>
        </div>
      </article>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
      <div class="shell-card space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="shell-eyebrow">Quickstart path</p>
            <h2 class="text-xl font-semibold text-slate-950">快速开始</h2>
          </div>
          <div class="shell-icon-badge shell-icon-badge--blue">
            <Zap class="h-5 w-5" />
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-3">
          <article class="shell-step-card" :class="{ 'is-active': activeStep === 0 }">
            <span class="shell-step-card__index">01</span>
            <h3>创建知识库</h3>
            <p>先为资料建立一个容器，把不同主题、项目或客户分开管理。</p>
          </article>
          <article class="shell-step-card" :class="{ 'is-active': activeStep === 1 }">
            <span class="shell-step-card__index">02</span>
            <h3>上传资料</h3>
            <p>把 Markdown、PDF、Word 等资料导入，让系统形成可检索资产。</p>
          </article>
          <article class="shell-step-card" :class="{ 'is-active': activeStep === 2 }">
            <span class="shell-step-card__index">03</span>
            <h3>开始问答</h3>
            <p>进入聊天页面，基于检索结果和会话记忆持续推进任务。</p>
          </article>
        </div>
      </div>

      <div class="shell-card space-y-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="shell-eyebrow">Next actions</p>
            <h2 class="text-xl font-semibold text-slate-950">推荐继续做的事</h2>
          </div>
          <div class="shell-icon-badge shell-icon-badge--mint">
            <ArrowRight class="h-5 w-5" />
          </div>
        </div>

        <button class="shell-list-action" @click="$router.push('/documents')">
          <div>
            <h3>打开文档工作区</h3>
            <p>查看文档解析、摘要和检索资产是否已经沉淀完成。</p>
          </div>
          <ArrowRight class="h-4 w-4" />
        </button>

        <button class="shell-list-action" @click="$router.push('/knowledge-base')">
          <div>
            <h3>切换知识库空间</h3>
            <p>为不同项目建立独立空间，避免摘要和对话串线。</p>
          </div>
          <ArrowRight class="h-4 w-4" />
        </button>

        <button class="shell-list-action" @click="$router.push('/retrieval-debug')">
          <div>
            <h3>检查检索调试链路</h3>
            <p>确认 chunk、向量和问答引用是否符合预期。</p>
          </div>
          <ArrowRight class="h-4 w-4" />
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Activity,
  ArrowRight,
  Bot,
  Database,
  FileText,
  FolderTree,
  Layers,
  MessageSquare,
  Sparkles,
  Upload,
  Zap
} from 'lucide-vue-next'
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

const progressLabel = computed(() => {
  if (activeStep.value === 0) return '准备建库'
  if (activeStep.value === 1) return '等待沉淀资料'
  return '可直接进入问答'
})

const progressDescription = computed(() => {
  if (activeStep.value === 0) return '先创建至少一个知识库空间，后续上传的资料和对话都会围绕它组织。'
  if (activeStep.value === 1) return '知识库已经准备好，下一步建议上传文档并等待摘要和检索资产生成。'
  return '核心链路已经具备，可以继续补充资料、调试检索并直接进入 AI 对话。'
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

    const categoryTrees = await Promise.all(kbIds.map((kbId) => getCategoryTree(kbId).catch(() => [])))
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

<style scoped>
.shell-card {
  position: relative;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92)),
    rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(18px);
  border-radius: 30px;
  padding: 28px;
  box-shadow:
    0 24px 60px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.shell-hero {
  min-height: 320px;
  background:
    radial-gradient(circle at top left, rgba(96, 165, 250, 0.16), transparent 34%),
    radial-gradient(circle at bottom right, rgba(245, 158, 11, 0.14), transparent 38%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.92));
}

.shell-hero__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(16px);
  opacity: 0.45;
}

.shell-hero__glow--blue {
  right: -40px;
  top: -10px;
  height: 180px;
  width: 180px;
  background: rgba(96, 165, 250, 0.22);
}

.shell-hero__glow--gold {
  bottom: -40px;
  left: 30%;
  height: 160px;
  width: 160px;
  background: rgba(251, 191, 36, 0.2);
}

.shell-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  background: rgba(255, 255, 255, 0.76);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2563eb;
}

.shell-inline-metric {
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.72);
  padding: 16px 18px;
}

.shell-inline-metric__label {
  display: block;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.shell-inline-metric__value {
  display: block;
  margin-top: 8px;
  font-size: 16px;
  color: #0f172a;
}

.shell-primary-btn,
.shell-secondary-btn,
.shell-list-action {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.shell-primary-btn,
.shell-secondary-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 600;
}

.shell-primary-btn {
  border: 1px solid rgba(59, 130, 246, 0.24);
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: white;
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.24);
}

.shell-secondary-btn {
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.82);
  color: #0f172a;
}

.shell-primary-btn:hover,
.shell-secondary-btn:hover,
.shell-list-action:hover {
  transform: translateY(-1px);
}

.shell-eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #94a3b8;
}

.shell-icon-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  width: 42px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.shell-icon-badge--gold {
  background: rgba(254, 243, 199, 0.72);
  color: #b45309;
}

.shell-icon-badge--blue {
  background: rgba(219, 234, 254, 0.72);
  color: #1d4ed8;
}

.shell-icon-badge--mint {
  background: rgba(220, 252, 231, 0.7);
  color: #15803d;
}

.shell-status-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.85);
  padding: 14px 16px;
}

.shell-status-line__label {
  font-size: 14px;
  color: #64748b;
}

.shell-status-line__value {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.shell-divider {
  height: 1px;
  width: 100%;
  background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.28), transparent);
}

.shell-progress {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.7);
}

.shell-progress__bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2563eb, #38bdf8 55%, #34d399);
}

.shell-stat-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.84);
  padding: 24px;
  box-shadow:
    0 18px 40px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.shell-stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;
  height: 46px;
  width: 46px;
  border-radius: 16px;
}

.shell-stat-card__icon--blue {
  background: rgba(219, 234, 254, 0.78);
  color: #2563eb;
}

.shell-stat-card__icon--green {
  background: rgba(220, 252, 231, 0.74);
  color: #15803d;
}

.shell-stat-card__icon--amber {
  background: rgba(254, 243, 199, 0.76);
  color: #d97706;
}

.shell-stat-card__icon--slate {
  background: rgba(226, 232, 240, 0.84);
  color: #334155;
}

.shell-step-card {
  border-radius: 26px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(248, 250, 252, 0.82);
  padding: 20px;
  min-height: 180px;
}

.shell-step-card.is-active {
  border-color: rgba(59, 130, 246, 0.24);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.92), rgba(248, 250, 252, 0.9));
  box-shadow: 0 18px 30px rgba(59, 130, 246, 0.08);
}

.shell-step-card__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #475569;
}

.shell-step-card h3 {
  margin-bottom: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.shell-step-card p {
  font-size: 14px;
  line-height: 1.75;
  color: #64748b;
}

.shell-list-action {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.78);
  padding: 18px 20px;
  text-align: left;
}

.shell-list-action h3 {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.shell-list-action p {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.dashboard-hero-title {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.16;
  letter-spacing: -0.035em;
}

@media (max-width: 768px) {
  .dashboard-hero-title {
    font-size: 2rem;
    line-height: 1.22;
    letter-spacing: -0.025em;
  }
}
</style>
