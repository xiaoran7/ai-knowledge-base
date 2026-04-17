<template>
  <div class="auth-page">
    <div class="auth-grid auth-grid--reverse">
      <section class="auth-panel">
        <div class="space-y-2">
          <p class="auth-eyebrow">Register</p>
          <h2 class="text-3xl font-semibold text-slate-950">创建账号</h2>
          <p class="text-sm leading-7 text-slate-500">
            创建一个新的工作身份，开始管理知识库、文档摘要和 AI 对话。
          </p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="space-y-5" label-position="top" @submit.prevent="handleRegister">
          <el-form-item prop="username">
            <template #label>
              <span class="auth-label">用户名</span>
            </template>
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="email">
            <template #label>
              <span class="auth-label">邮箱</span>
            </template>
            <el-input
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱地址"
              size="large"
              :prefix-icon="Mail"
            />
          </el-form-item>

          <el-form-item prop="password">
            <template #label>
              <span class="auth-label">密码</span>
            </template>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <template #label>
              <span class="auth-label">确认密码</span>
            </template>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <button type="button" :disabled="loading" class="auth-primary-btn w-full" @click="handleRegister">
            <Loader2 v-if="loading" class="h-5 w-5 animate-spin" />
            <span v-else>创建并进入登录页</span>
          </button>
        </el-form>

        <div class="auth-inline-note">
          <span>已经有账号了？</span>
          <button type="button" class="auth-link" @click="goToLogin">去登录</button>
        </div>
      </section>

      <section class="auth-showcase">
        <div class="auth-showcase__glow auth-showcase__glow--blue"></div>
        <div class="auth-showcase__glow auth-showcase__glow--mint"></div>
        <div class="relative z-10 space-y-8">
          <span class="auth-tag">
            <Sparkles class="h-4 w-4" />
            Build your workspace
          </span>
          <div class="space-y-4">
            <h1 class="auth-showcase-title text-slate-950">
              为你的资料建立一个可持续演进的 AI 工作台。
            </h1>
            <p class="max-w-xl text-base leading-8 text-slate-600">
              注册后你可以创建知识库空间、上传文档、沉淀摘要资产，并在统一界面里把检索和问答串起来。
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-3">
            <article class="auth-feature">
              <Layers3 class="h-5 w-5" />
              <div>
                <h3>分层沉淀</h3>
                <p>原文、摘要和向量检索资产各自独立，方便后续维护。</p>
              </div>
            </article>
            <article class="auth-feature">
              <MessagesSquare class="h-5 w-5" />
              <div>
                <h3>连续会话</h3>
                <p>会话可以带着摘要和事实记忆持续推进，不再只是一次性问答。</p>
              </div>
            </article>
            <article class="auth-feature">
              <LayoutDashboard class="h-5 w-5" />
              <div>
                <h3>统一工作台</h3>
                <p>Dashboard、知识库、文档和聊天页面保持同一套视觉语言。</p>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import {
  LayoutDashboard,
  Layers3,
  Loader2,
  Lock,
  Mail,
  MessagesSquare,
  Sparkles,
  User
} from 'lucide-vue-next'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度应为 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate()

  loading.value = true
  try {
    await register({
      username: form.username,
      email: form.email,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) {
    ElMessage.error(error?.response?.data || '注册失败')
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(125, 211, 252, 0.16), transparent 26%),
    radial-gradient(circle at bottom right, rgba(34, 197, 94, 0.1), transparent 26%),
    linear-gradient(180deg, #f0fdf4 0%, #f8fafc 48%, #eef2ff 100%);
  padding: 32px;
}

.auth-grid,
.auth-grid--reverse {
  display: grid;
  min-height: calc(100vh - 64px);
  gap: 24px;
  grid-template-columns: minmax(360px, 460px) minmax(0, 1.15fr);
}

.auth-showcase,
.auth-panel {
  position: relative;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 34px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 250, 252, 0.92)),
    rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(18px);
  box-shadow:
    0 28px 70px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.auth-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 34px;
}

.auth-showcase {
  overflow: hidden;
  padding: 48px;
  display: flex;
  align-items: center;
}

.auth-showcase__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(16px);
  opacity: 0.42;
}

.auth-showcase__glow--blue {
  top: -28px;
  right: -20px;
  width: 220px;
  height: 220px;
  background: rgba(96, 165, 250, 0.24);
}

.auth-showcase__glow--mint {
  left: 8%;
  bottom: -56px;
  width: 190px;
  height: 190px;
  background: rgba(34, 197, 94, 0.16);
}

.auth-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(34, 197, 94, 0.18);
  background: rgba(255, 255, 255, 0.78);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #15803d;
}

.auth-eyebrow {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #94a3b8;
}

.auth-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.auth-feature {
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.78);
  padding: 18px;
  color: #0f172a;
}

.auth-feature h3 {
  margin-top: 14px;
  font-size: 16px;
  font-weight: 600;
}

.auth-feature p {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.auth-primary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 18px;
  border: 1px solid rgba(34, 197, 94, 0.22);
  background: linear-gradient(135deg, #16a34a, #22c55e);
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 600;
  color: white;
  box-shadow: 0 18px 36px rgba(22, 163, 74, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.auth-primary-btn:hover {
  transform: translateY(-1px);
}

.auth-primary-btn:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.auth-inline-note {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.auth-link {
  color: #16a34a;
  font-weight: 600;
}

.auth-showcase-title {
  font-size: 3.05rem;
  font-weight: 700;
  line-height: 1.14;
  letter-spacing: -0.038em;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.el-input__wrapper) {
  border-radius: 18px;
  padding: 4px 14px;
  background: rgba(248, 250, 252, 0.9);
  box-shadow: none;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

:deep(.el-input__prefix) {
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .auth-grid,
  .auth-grid--reverse {
    grid-template-columns: 1fr;
  }

  .auth-showcase {
    min-height: 420px;
  }

  .auth-showcase-title {
    font-size: 2.45rem;
    line-height: 1.18;
    letter-spacing: -0.028em;
  }
}

@media (max-width: 640px) {
  .auth-page {
    padding: 20px;
  }

  .auth-showcase,
  .auth-panel {
    border-radius: 28px;
  }

  .auth-showcase {
    padding: 32px 24px;
  }

  .auth-panel {
    padding: 28px 22px;
  }

  .auth-showcase-title {
    font-size: 2rem;
    line-height: 1.22;
    letter-spacing: -0.02em;
  }
}
</style>
