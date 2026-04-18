<template>
  <div class="mx-auto flex w-full max-w-7xl flex-col gap-8 px-6 py-8 text-slate-900">
    <section class="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
      <div class="kb-card kb-hero overflow-hidden">
        <div class="kb-hero__orb kb-hero__orb--blue"></div>
        <div class="kb-hero__orb kb-hero__orb--mint"></div>
        <div class="relative z-10 space-y-5">
          <span class="kb-tag">
            <Library class="h-4 w-4" />
            Knowledge spaces
          </span>
          <div class="space-y-3">
            <h1 class="kb-hero-title text-slate-950">用独立知识库把资料、摘要和对话上下文分层整理。</h1>
            <p class="max-w-2xl text-sm leading-7 text-slate-600 md:text-[15px]">
              每个知识库都是一个可持续工作的空间。你可以按项目、客户、主题或团队隔离资料，避免检索结果和会话记忆彼此污染。
            </p>
          </div>

          <div class="flex flex-wrap gap-3">
            <button class="kb-primary-btn" @click="showCreateDialog">
              <Plus class="h-4 w-4" />
              新建知识库
            </button>
            <button class="kb-secondary-btn" @click="$router.push('/documents')">
              <FileStack class="h-4 w-4" />
              前往文档管理
            </button>
          </div>
        </div>
      </div>

      <div class="kb-card space-y-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="kb-eyebrow">Overview</p>
            <h2 class="text-xl font-semibold text-slate-950">空间概览</h2>
          </div>
          <div class="kb-icon-badge kb-icon-badge--blue">
            <Database class="h-5 w-5" />
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <article class="kb-stat">
            <span class="kb-stat__label">知识库总数</span>
            <strong class="kb-stat__value">{{ knowledgeBaseList.length }}</strong>
          </article>
          <article class="kb-stat">
            <span class="kb-stat__label">当前选中</span>
            <strong class="kb-stat__value">{{ knowledgeStore.currentKb?.name || '未选择' }}</strong>
          </article>
        </div>

        <div class="kb-divider"></div>

        <div class="space-y-3 text-sm leading-7 text-slate-600">
          <p>建议按业务边界建库，而不是把所有资料堆进同一个空间。</p>
          <p>这样可以让摘要、向量检索和会话记忆都更稳定，也更方便后续做权限和归档扩展。</p>
        </div>
      </div>
    </section>

    <section class="kb-card overflow-hidden" v-loading="loading">
      <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <p class="kb-eyebrow">Knowledge base list</p>
          <h2 class="text-xl font-semibold text-slate-950">管理知识库</h2>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="kb-secondary-btn" @click="loadData">
            <RefreshCw class="h-4 w-4" />
            刷新列表
          </button>
          <button class="kb-primary-btn" @click="showCreateDialog">
            <Plus class="h-4 w-4" />
            新建知识库
          </button>
        </div>
      </div>

      <div v-if="knowledgeBaseList.length === 0" class="kb-empty">
        <div class="kb-empty__icon">
          <FolderKanban class="h-7 w-7" />
        </div>
        <h3>还没有知识库空间</h3>
        <p>先建一个容器，后续上传的文档、摘要和对话都会围绕它组织。</p>
        <button class="kb-primary-btn" @click="showCreateDialog">
          <Plus class="h-4 w-4" />
          立即创建
        </button>
      </div>

      <div v-else class="grid gap-4 xl:grid-cols-2">
        <article
          v-for="kb in knowledgeBaseList"
          :key="kb.id"
          class="kb-list-card"
          :class="{ 'is-current': knowledgeStore.currentKbId === kb.id }"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="space-y-3">
              <div class="inline-flex rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
                {{ knowledgeStore.currentKbId === kb.id ? '当前空间' : '知识空间' }}
              </div>
              <div>
                <h3 class="text-xl font-semibold text-slate-950">{{ kb.name }}</h3>
                <p class="mt-2 text-sm leading-7 text-slate-600">
                  {{ kb.description || '这个知识库还没有描述，可以补充它的业务范围、资料边界或适用场景。' }}
                </p>
              </div>
            </div>
            <div class="kb-list-card__count">
              <span>文档数</span>
              <strong>{{ kb.documentCount ?? 0 }}</strong>
            </div>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <div class="kb-meta">
              <span class="kb-meta__label">创建时间</span>
              <strong class="kb-meta__value">{{ formatDate(kb.createdAt) }}</strong>
            </div>
            <div class="kb-meta">
              <span class="kb-meta__label">知识状态</span>
              <strong class="kb-meta__value">{{ knowledgeStore.currentKbId === kb.id ? '已接入当前工作区' : '可切换使用' }}</strong>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button class="kb-primary-btn" @click="handleEnter(kb)">
              <ArrowRight class="h-4 w-4" />
              进入空间
            </button>
            <button class="kb-secondary-btn" @click="handleEdit(kb)">
              <Edit class="h-4 w-4" />
              编辑说明
            </button>
            <button class="kb-danger-btn" @click="handleDelete(kb)">
              <Trash2 class="h-4 w-4" />
              删除
            </button>
          </div>
        </article>
      </div>
    </section>

    <el-dialog
      v-model="createDialogVisible"
      :title="dialogMode === 'create' ? '\u65b0\u5efa\u77e5\u8bc6\u5e93' : '\u7f16\u8f91\u77e5\u8bc6\u5e93'"
      width="560px"
      append-to-body
      destroy-on-close
      class="kb-dialog"
    >
      <div class="space-y-6">
        <div class="rounded-[24px] border border-slate-200/70 bg-slate-50/80 p-5">
          <p class="text-sm leading-7 text-slate-600">
            为这个空间取一个稳定的名字，并说明它的资料范围。后续上传的文档、摘要和问答上下文都会围绕这里组织。
          </p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submitForm">
          <el-form-item label="名称" prop="name">
            <el-input v-model="form.name" placeholder="例如：编程竞赛知识库 / 客户支持 FAQ" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="补充说明资料边界、检索用途和适用场景"
            />
          </el-form-item>
        </el-form>

        <div class="flex justify-end gap-3">
          <button class="kb-secondary-btn" @click="createDialogVisible = false">取消</button>
          <button class="kb-primary-btn" :disabled="submitLoading" @click="submitForm">
            <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
            <span v-else>保存知识库</span>
          </button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowRight,
  Database,
  Edit,
  FileStack,
  FolderKanban,
  Library,
  Loader2,
  Plus,
  RefreshCw,
  Trash2
} from 'lucide-vue-next'
import { useKnowledgeStore } from '@/stores/knowledge'
import type { KnowledgeBase } from '@/api/knowledge'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const loading = ref(false)
const knowledgeBaseList = ref<KnowledgeBase[]>([])

const createDialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = ref({
  id: '',
  name: '',
  description: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '\u8bf7\u8f93\u5165\u77e5\u8bc6\u5e93\u540d\u79f0', trigger: 'blur' },
    { min: 2, max: 50, message: '\u957f\u5ea6\u5728 2 \u5230 50 \u4e2a\u5b57\u7b26', trigger: 'blur' }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    knowledgeBaseList.value = await knowledgeStore.fetchKnowledgeBaseList()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const showCreateDialog = () => {
  dialogMode.value = 'create'
  form.value = { id: '', name: '', description: '' }
  createDialogVisible.value = true
}

const handleEdit = (kb: KnowledgeBase) => {
  dialogMode.value = 'edit'
  form.value = {
    id: kb.id,
    name: kb.name,
    description: kb.description || ''
  }
  createDialogVisible.value = true
}

const handleEnter = (kb: KnowledgeBase) => {
  knowledgeStore.setCurrentKb(kb.id)
  router.push(`/knowledge-base/${kb.id}`)
}

const handleDelete = (kb: KnowledgeBase) => {
  ElMessageBox.confirm(
    `\u786e\u8ba4\u5220\u9664\u77e5\u8bc6\u5e93 "${kb.name}" \u5417\uff1f\u6b64\u64cd\u4f5c\u4e0d\u53ef\u6062\u590d\uff0c\u5173\u8054\u7684\u6587\u6863\u4e5f\u4f1a\u88ab\u5220\u9664\u3002`,
    '\u5220\u9664\u786e\u8ba4',
    {
      type: 'warning',
      confirmButtonText: '\u5220\u9664',
      cancelButtonText: '\u53d6\u6d88'
    }
  ).then(async () => {
    try {
      await knowledgeStore.deleteKb(kb.id)
      ElMessage.success('\u5220\u9664\u6210\u529f')
      await loadData()
    } catch (_error) {
      // Error handled by interceptor.
    }
  })
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (dialogMode.value === 'create') {
        await knowledgeStore.createKb(form.value.name, form.value.description)
        ElMessage.success('\u521b\u5efa\u6210\u529f')
      } else {
        await knowledgeStore.updateKb(form.value.id, form.value.name, form.value.description)
        ElMessage.success('\u4fee\u6539\u6210\u529f')
      }
      createDialogVisible.value = false
      await loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

function formatDate(value?: string) {
  if (!value) return '\u672a\u77e5'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}
</script>

<style scoped>
.kb-card {
  position: relative;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.9)),
    rgba(255, 255, 255, 0.88);
  padding: 28px;
  box-shadow:
    0 24px 60px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(18px);
}

.kb-hero {
  min-height: 280px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.16), transparent 32%),
    radial-gradient(circle at bottom right, rgba(45, 212, 191, 0.14), transparent 34%),
    linear-gradient(150deg, rgba(255, 255, 255, 0.98), rgba(240, 253, 250, 0.88));
}

.kb-hero__orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.45;
}

.kb-hero__orb--blue {
  top: -40px;
  right: -20px;
  width: 180px;
  height: 180px;
  background: rgba(59, 130, 246, 0.22);
}

.kb-hero__orb--mint {
  bottom: -40px;
  left: 24%;
  width: 150px;
  height: 150px;
  background: rgba(20, 184, 166, 0.18);
}

.kb-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  background: rgba(255, 255, 255, 0.78);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2563eb;
}

.kb-primary-btn,
.kb-secondary-btn,
.kb-danger-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 999px;
  padding: 11px 18px;
  font-size: 14px;
  font-weight: 600;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.kb-primary-btn:hover,
.kb-secondary-btn:hover,
.kb-danger-btn:hover {
  transform: translateY(-1px);
}

.kb-primary-btn {
  border: 1px solid rgba(59, 130, 246, 0.2);
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: white;
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.2);
}

.kb-primary-btn:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.kb-secondary-btn {
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.8);
  color: #0f172a;
}

.kb-danger-btn {
  border: 1px solid rgba(248, 113, 113, 0.2);
  background: rgba(254, 242, 242, 0.86);
  color: #dc2626;
}

.kb-eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #94a3b8;
}

.kb-icon-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.kb-icon-badge--blue {
  background: rgba(219, 234, 254, 0.78);
  color: #1d4ed8;
}

.kb-stat {
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(248, 250, 252, 0.82);
  padding: 18px;
}

.kb-stat__label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.kb-stat__value {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  color: #0f172a;
}

.kb-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.28), transparent);
}

.kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  border-radius: 26px;
  border: 1px dashed rgba(148, 163, 184, 0.36);
  background: rgba(248, 250, 252, 0.74);
  padding: 52px 20px;
  text-align: center;
}

.kb-empty__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 20px;
  background: rgba(219, 234, 254, 0.82);
  color: #2563eb;
}

.kb-empty h3 {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
}

.kb-empty p {
  max-width: 460px;
  font-size: 14px;
  line-height: 1.8;
  color: #64748b;
}

.kb-list-card {
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.84);
  padding: 24px;
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.kb-list-card.is-current {
  border-color: rgba(59, 130, 246, 0.24);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.9), rgba(255, 255, 255, 0.9));
}

.kb-list-card__count {
  min-width: 92px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
  padding: 14px 12px;
  text-align: center;
}

.kb-list-card__count span {
  display: block;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #94a3b8;
}

.kb-list-card__count strong {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: #0f172a;
}

.kb-meta {
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.84);
  padding: 14px 16px;
}

.kb-meta__label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.kb-meta__value {
  display: block;
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.kb-hero-title {
  font-size: 2.35rem;
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.03em;
}

:deep(.kb-dialog .el-dialog) {
  border-radius: 30px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94));
  box-shadow: 0 32px 80px rgba(15, 23, 42, 0.18);
}

:deep(.kb-dialog .el-dialog__header) {
  padding: 24px 28px 0;
}

:deep(.kb-dialog .el-dialog__body) {
  padding: 20px 28px 28px;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 18px;
  box-shadow: none;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

@media (max-width: 768px) {
  .kb-hero-title {
    font-size: 1.92rem;
    line-height: 1.24;
    letter-spacing: -0.02em;
  }
}
</style>


