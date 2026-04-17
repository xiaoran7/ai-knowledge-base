<template>
  <div class="auth-page">
    <div class="auth-grid">
      <section class="auth-showcase">
        <div class="auth-showcase__glow auth-showcase__glow--blue"></div>
        <div class="auth-showcase__glow auth-showcase__glow--gold"></div>
        <div class="relative z-10 space-y-8">
          <span class="auth-tag">
            <Sparkles class="h-4 w-4" />
            AI Knowledge Base
          </span>
          <div class="space-y-4">
            <h1 class="auth-showcase-title text-slate-950">
              欢迎回来，继续你上次停下的知识工作流。
            </h1>
            <p class="max-w-xl text-base leading-8 text-slate-600">
              登录后你可以继续上传资料、查看摘要资产、维护知识库空间，并让 AI 在有上下文的对话里持续回答问题。
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-3">
            <article class="auth-feature">
              <BookOpen class="h-5 w-5" />
              <div>
                <h3>资料沉淀</h3>
                <p>文档、摘要和检索资产在同一工作流里持续更新。</p>
              </div>
            </article>
            <article class="auth-feature">
              <BrainCircuit class="h-5 w-5" />
              <div>
                <h3>检索增强问答</h3>
                <p>优先结合知识库回答，没有命中时再回落通用能力。</p>
              </div>
            </article>
            <article class="auth-feature">
              <ShieldCheck class="h-5 w-5" />
              <div>
                <h3>私有空间隔离</h3>
                <p>每个知识库独立管理资料和对话，减少串话风险。</p>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="space-y-2">
          <p class="auth-eyebrow">Sign in</p>
          <h2 class="text-3xl font-semibold text-slate-950">登录账号</h2>
          <p class="text-sm leading-7 text-slate-500">
            输入用户名和密码，回到你的知识工作台。
          </p>
        </div>

        <el-form class="space-y-5" label-position="top" @submit.prevent="handleLogin">
          <el-form-item>
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

          <el-form-item>
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

          <button type="submit" :disabled="loading" class="auth-primary-btn w-full">
            <Loader2 v-if="loading" class="h-5 w-5 animate-spin" />
            <span v-else>进入工作台</span>
          </button>
        </el-form>

        <div class="auth-inline-note">
          <span>还没有账号？</span>
          <button type="button" class="auth-link" @click="goToRegister">去注册</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { BookOpen, BrainCircuit, Loader2, Lock, ShieldCheck, Sparkles, User } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    await userStore.loginAction(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    ElMessage.error(error?.response?.data || '登录失败')
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(125, 211, 252, 0.16), transparent 26%),
    radial-gradient(circle at bottom right, rgba(251, 191, 36, 0.14), transparent 28%),
    linear-gradient(180deg, #eff6ff 0%, #f8fafc 48%, #f1f5f9 100%);
  padding: 32px;
}

.auth-grid {
  display: grid;
  min-height: calc(100vh - 64px);
  gap: 24px;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 460px);
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
  top: -32px;
  left: -18px;
  width: 220px;
  height: 220px;
  background: rgba(96, 165, 250, 0.24);
}

.auth-showcase__glow--gold {
  right: 8%;
  bottom: -48px;
  width: 180px;
  height: 180px;
  background: rgba(250, 204, 21, 0.18);
}

.auth-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  background: rgba(255, 255, 255, 0.76);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2563eb;
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

.auth-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 34px;
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

.auth-primary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 18px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 600;
  color: white;
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.22);
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
  color: #2563eb;
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
  .auth-grid {
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
