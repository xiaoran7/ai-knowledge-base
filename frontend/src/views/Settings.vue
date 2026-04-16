<template>
  <div class="settings page-container">
    <div class="page-header">
      <div>
        <h2>设置</h2>
        <p class="page-subtitle">管理个人资料、AI 头像、LLM 配置和项目运行信息。</p>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="10">
        <el-card class="mb-4 profile-card">
          <template #header>
            <span>个人资料</span>
          </template>

          <div class="profile-top">
            <el-avatar :size="84" :src="avatarPreview || undefined" class="profile-avatar">
              {{ userInitial }}
            </el-avatar>
            <div class="profile-summary">
              <div class="profile-name">{{ userStore.username }}</div>
              <div class="profile-email">{{ userStore.user?.email }}</div>
              <div class="profile-role">{{ userStore.user?.role || 'ROLE_USER' }}</div>
            </div>
          </div>

          <div class="avatar-actions">
            <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="handleAvatarSelect">
              <el-button :loading="avatarUploading">上传头像</el-button>
            </el-upload>
            <el-button plain :disabled="!userStore.user?.avatarUrl" :loading="profileSaving" @click="handleClearAvatar">
              清除头像
            </el-button>
          </div>
          <div class="avatar-tip">支持上传本地图片并保存到项目本地目录，建议使用较小的方形头像。</div>

          <div class="assistant-avatar-panel">
            <div class="assistant-avatar-header">
              <el-avatar :size="64" :src="assistantAvatarPreview || undefined" class="profile-avatar">AI</el-avatar>
              <div class="assistant-avatar-copy">
                <div class="assistant-avatar-title">AI 助手头像</div>
                <div class="assistant-avatar-subtitle">聊天页中的 AI 回复头像会使用这里的图片。</div>
              </div>
            </div>
            <div class="avatar-actions">
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                accept="image/*"
                :on-change="handleAssistantAvatarSelect"
              >
                <el-button :loading="assistantAvatarUploading">上传 AI 头像</el-button>
              </el-upload>
              <el-button
                plain
                :disabled="!userStore.user?.assistantAvatarUrl"
                :loading="profileSaving"
                @click="handleClearAssistantAvatar"
              >
                清除 AI 头像
              </el-button>
            </div>
          </div>

          <el-form label-position="top">
            <el-form-item label="个人简介">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="5"
                maxlength="1000"
                show-word-limit
                placeholder="介绍一下你的角色、领域、偏好或使用目标"
              />
            </el-form-item>
            <div class="profile-actions">
              <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存资料</el-button>
              <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
            </div>
          </el-form>
        </el-card>

        <el-card class="mb-4">
          <template #header>
            <span>账户信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
            <el-descriptions-item label="用户 ID">{{ userStore.user?.userId }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userStore.user?.email }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ userStore.user?.createdAt || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card>
          <template #header>
            <span>本地 Embedding</span>
          </template>
          <el-alert
            title="当前向量检索使用项目内置本地轻量 embedding 模型"
            type="success"
            :closable="false"
            description="模型文件位于 backend/models/bge-small-zh-v1.5-onnx，当前不依赖远程 embedding API 或本地 Ollama。"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card class="mb-4">
          <template #header>
            <div class="card-header">
              <span>LLM API 配置</span>
              <el-button type="primary" size="small" @click="showAddConfigDialog">
                <el-icon><Plus /></el-icon>
                添加配置
              </el-button>
            </div>
          </template>

          <el-empty
            v-if="llmConfigs.length === 0"
            description="暂无 LLM 配置，请先添加 API Key 后再使用 AI 能力"
          />

          <el-table v-else :data="llmConfigs" style="width: 100%">
            <el-table-column prop="providerName" label="提供商" width="140" />
            <el-table-column prop="modelName" label="对话模型" min-width="150">
              <template #default="{ row }">
                <el-tag size="small">{{ row.modelName || '默认模型' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="temperature" label="温度" width="90" />
            <el-table-column prop="maxTokens" label="最大 Token" width="120" />
            <el-table-column label="默认" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="90">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.isEnabled"
                  @change="(enabled: string | number | boolean) => handleToggleEnabled(row, enabled)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!row.isDefault" link type="primary" @click="handleSetDefault(row)">设为默认</el-button>
                <el-button link type="primary" @click="showEditConfigDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDeleteConfig(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="mb-4">
          <template #header>
            <span>支持的模型提供商</span>
          </template>
          <el-table :data="providers" style="width: 100%">
            <el-table-column prop="name" label="提供商" width="170" />
            <el-table-column prop="defaultBaseUrl" label="默认 API 地址" min-width="220" />
            <el-table-column prop="models" label="支持模型" min-width="220">
              <template #default="{ row }">
                <el-tag v-for="model in row.models" :key="model" size="small" class="model-tag">
                  {{ model }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card>
          <template #header>
            <span>关于项目</span>
          </template>
          <div class="about-content">
            <p><strong>AI 知识库</strong> 是一个基于 RAG 的个人知识管理与问答系统。</p>
            <p>前端：Vue 3 + Element Plus</p>
            <p>后端：Spring Boot + PostgreSQL + Redis</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="configDialogVisible" :title="editConfigId ? '编辑 LLM 配置' : '添加 LLM 配置'" width="560px">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="110px">
        <el-form-item label="提供商" prop="provider">
          <el-select
            v-model="configForm.provider"
            placeholder="选择提供商"
            :disabled="!!editConfigId"
            @change="handleProviderChange"
          >
            <el-option
              v-for="provider in providers"
              :key="provider.provider"
              :label="provider.name"
              :value="provider.provider"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="configForm.apiKey" type="password" show-password placeholder="输入 API Key" />
        </el-form-item>
        <el-form-item label="API 地址">
          <el-input v-model="configForm.apiBaseUrl" :placeholder="defaultBaseUrl" />
        </el-form-item>
        <el-form-item label="对话模型">
          <el-select v-model="configForm.modelName" placeholder="选择对话模型" clearable filterable>
            <el-option v-for="model in currentModels" :key="model" :label="model" :value="model" />
          </el-select>
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="configForm.temperature" :min="0" :max="1" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="最大 Token">
          <el-input-number v-model="configForm.maxTokens" :min="256" :max="128000" :step="256" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="configForm.isDefault" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="configForm.remark" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="configLoading" @click="handleSaveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  createLlmConfig,
  deleteLlmConfig,
  getLlmConfigs,
  getLlmProviders,
  setDefaultLlmConfig,
  setLlmConfigEnabled,
  updateLlmConfig,
  type LlmConfig,
  type LlmProvider
} from '@/api/llm'

const router = useRouter()
const userStore = useUserStore()

const providers = ref<LlmProvider[]>([])
const llmConfigs = ref<LlmConfig[]>([])

const configDialogVisible = ref(false)
const configFormRef = ref<FormInstance>()
const editConfigId = ref('')
const configLoading = ref(false)

const profileSaving = ref(false)
const avatarUploading = ref(false)
const assistantAvatarUploading = ref(false)
const profileForm = reactive({
  bio: ''
})

const configForm = reactive({
  provider: '',
  apiKey: '',
  apiBaseUrl: '',
  modelName: '',
  isDefault: false,
  temperature: 0.7,
  maxTokens: 4096,
  remark: ''
})

const configRules: FormRules = {
  provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
}

const currentProvider = computed(() =>
  providers.value.find((provider) => provider.provider === configForm.provider) || null
)
const defaultBaseUrl = computed(() => currentProvider.value?.defaultBaseUrl || '')
const currentModels = computed(() => currentProvider.value?.models || [])
const userInitial = computed(() => (userStore.username || 'U').charAt(0).toUpperCase())

function resolveAssetUrl(url?: string) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  return `/api${url}`
}

const avatarPreview = computed(() => resolveAssetUrl(userStore.user?.avatarUrl))
const assistantAvatarPreview = computed(() => resolveAssetUrl(userStore.user?.assistantAvatarUrl))

function normalizeProviderBaseUrl(provider: string, apiBaseUrl?: string) {
  const value = (apiBaseUrl || '').trim()
  const lower = value.toLowerCase()
  if (provider === 'minimax' || lower.includes('minimax.io') || lower.includes('minimaxi.com')) {
    return 'https://api.minimaxi.com/v1'
  }
  return value
}

function syncProfileForm() {
  profileForm.bio = userStore.user?.bio || ''
}

onMounted(async () => {
  await Promise.all([loadProviders(), loadConfigs(), userStore.hydrateUser().catch(() => null)])
  syncProfileForm()
})

watch(
  () => userStore.user,
  () => {
    syncProfileForm()
  },
  { deep: true }
)

async function loadProviders() {
  providers.value = await getLlmProviders()
}

async function loadConfigs() {
  llmConfigs.value = await getLlmConfigs()
}

function resetForm() {
  configForm.provider = ''
  configForm.apiKey = ''
  configForm.apiBaseUrl = ''
  configForm.modelName = ''
  configForm.isDefault = llmConfigs.value.length === 0
  configForm.temperature = 0.7
  configForm.maxTokens = 4096
  configForm.remark = ''
}

function showAddConfigDialog() {
  editConfigId.value = ''
  resetForm()
  configDialogVisible.value = true
}

function showEditConfigDialog(config: LlmConfig) {
  editConfigId.value = config.id
  configForm.provider = config.provider
  configForm.apiKey = ''
  configForm.apiBaseUrl = normalizeProviderBaseUrl(config.provider, config.apiBaseUrl)
  configForm.modelName = config.modelName || ''
  configForm.isDefault = config.isDefault
  configForm.temperature = config.temperature
  configForm.maxTokens = config.maxTokens
  configForm.remark = config.remark || ''
  configDialogVisible.value = true
}

function handleProviderChange() {
  configForm.apiBaseUrl = normalizeProviderBaseUrl(
    configForm.provider,
    configForm.apiBaseUrl || defaultBaseUrl.value
  )
}

async function handleSaveConfig() {
  if (!configFormRef.value) return
  await configFormRef.value.validate()

  configLoading.value = true
  try {
    const payload = {
      provider: configForm.provider,
      apiKey: configForm.apiKey,
      apiBaseUrl: normalizeProviderBaseUrl(configForm.provider, configForm.apiBaseUrl) || undefined,
      modelName: configForm.modelName || undefined,
      isDefault: configForm.isDefault,
      temperature: configForm.temperature,
      maxTokens: configForm.maxTokens,
      remark: configForm.remark || undefined
    }

    if (editConfigId.value) {
      await updateLlmConfig(editConfigId.value, payload)
      ElMessage.success('配置更新成功')
    } else {
      await createLlmConfig(payload)
      ElMessage.success('配置添加成功')
    }

    configDialogVisible.value = false
    await loadConfigs()
  } finally {
    configLoading.value = false
  }
}

function handleSetDefault(config: LlmConfig) {
  ElMessageBox.confirm(`确定将 ${config.providerName} 设为默认配置吗？`, '提示', {
    type: 'info'
  }).then(async () => {
    await setDefaultLlmConfig(config.id)
    ElMessage.success('默认配置已更新')
    await loadConfigs()
  })
}

async function handleToggleEnabled(config: LlmConfig, enabled: string | number | boolean) {
  await setLlmConfigEnabled(config.id, Boolean(enabled))
  ElMessage.success(Boolean(enabled) ? '配置已启用' : '配置已禁用')
  await loadConfigs()
}

function handleDeleteConfig(config: LlmConfig) {
  ElMessageBox.confirm(`确定删除 ${config.providerName} 配置吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteLlmConfig(config.id)
    ElMessage.success('配置已删除')
    await loadConfigs()
  })
}

async function handleAvatarSelect(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  avatarUploading.value = true
  try {
    await userStore.uploadAvatarAction(uploadFile.raw)
    ElMessage.success('头像上传成功')
  } finally {
    avatarUploading.value = false
  }
}

async function handleAssistantAvatarSelect(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  assistantAvatarUploading.value = true
  try {
    await userStore.uploadAssistantAvatarAction(uploadFile.raw)
    ElMessage.success('AI 头像上传成功')
  } finally {
    assistantAvatarUploading.value = false
  }
}

async function handleClearAvatar() {
  profileSaving.value = true
  try {
    await userStore.updateProfileAction({
      avatarUrl: '',
      assistantAvatarUrl: userStore.user?.assistantAvatarUrl || undefined,
      bio: profileForm.bio.trim() || undefined
    })
    ElMessage.success('头像已清除')
  } finally {
    profileSaving.value = false
  }
}

async function handleClearAssistantAvatar() {
  profileSaving.value = true
  try {
    await userStore.updateProfileAction({
      avatarUrl: userStore.user?.avatarUrl || undefined,
      assistantAvatarUrl: '',
      bio: profileForm.bio.trim() || undefined
    })
    ElMessage.success('AI 头像已清除')
  } finally {
    profileSaving.value = false
  }
}

async function handleSaveProfile() {
  profileSaving.value = true
  try {
    await userStore.updateProfileAction({
      avatarUrl: userStore.user?.avatarUrl || undefined,
      assistantAvatarUrl: userStore.user?.assistantAvatarUrl || undefined,
      bio: profileForm.bio.trim() || undefined
    })
    ElMessage.success('个人资料已保存')
  } finally {
    profileSaving.value = false
  }
}

async function handleLogout() {
  await userStore.logoutAction()
  router.push('/login')
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #223046;
}

.page-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #7b8ba1;
}

.mb-4 {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-card {
  overflow: hidden;
}

.profile-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 18px;
  border-radius: 14px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.15), transparent 32%),
    linear-gradient(180deg, #f7fbff 0%, #eef6ff 100%);
}

.profile-avatar {
  border: 3px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 28px rgba(64, 158, 255, 0.16);
}

.profile-summary {
  min-width: 0;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  color: #223046;
}

.profile-email,
.profile-role,
.assistant-avatar-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #6f8097;
}

.avatar-actions,
.profile-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.avatar-tip {
  margin-bottom: 18px;
  font-size: 12px;
  color: #7b8ba1;
}

.assistant-avatar-panel {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  background: #fbfdff;
}

.assistant-avatar-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.assistant-avatar-title {
  font-size: 15px;
  font-weight: 600;
  color: #223046;
}

.model-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.about-content {
  line-height: 1.9;
  color: #53657d;
}

.about-content p {
  margin: 0;
}

@media (max-width: 1200px) {
  .profile-actions,
  .avatar-actions {
    flex-wrap: wrap;
  }
}
</style>
