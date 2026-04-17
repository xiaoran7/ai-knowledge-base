<template>
  <div class="flex h-screen w-full overflow-hidden bg-white text-sm text-gray-900 dark:bg-gray-900 dark:text-gray-100">
    <aside
      class="flex h-full w-[280px] shrink-0 flex-col border-r border-gray-200 bg-gray-50/70 backdrop-blur-xl dark:border-white/10 dark:bg-black/50"
    >
      <div class="p-3">
        <button
          class="group flex w-full items-center gap-3 rounded-2xl border border-gray-200 bg-white px-3 py-3 text-left shadow-sm transition-all hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md dark:border-white/10 dark:bg-white/5 dark:hover:border-blue-500/30"
          @click="router.push('/chat')"
        >
          <div
            class="flex h-9 w-9 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-cyan-400 text-white shadow-sm"
          >
            <Bot class="h-4 w-4" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="font-medium text-gray-900 dark:text-white">New chat</div>
            <div class="truncate text-xs text-gray-500 dark:text-gray-400">开始一段新的知识问答</div>
          </div>
          <Edit class="h-4 w-4 text-gray-400 transition-transform group-hover:translate-x-0.5 dark:text-gray-500" />
        </button>
      </div>

      <div class="px-3 pb-2">
        <div class="rounded-2xl border border-gray-200 bg-white/80 p-2 shadow-sm dark:border-white/10 dark:bg-white/5">
          <div class="mb-2 px-2 pt-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-gray-400">
            Workspace
          </div>
          <button
            v-for="item in primaryNav"
            :key="item.path"
            class="mb-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-all last:mb-0"
            :class="isActive(item.path)
              ? 'bg-blue-50 text-blue-700 shadow-sm ring-1 ring-blue-100 dark:bg-blue-500/10 dark:text-blue-300 dark:ring-blue-500/20'
              : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-white/10 dark:hover:text-white'"
            @click="router.push(item.path)"
          >
            <component :is="item.icon" class="h-4 w-4 shrink-0" />
            <div class="min-w-0 flex-1">
              <div class="truncate font-medium">{{ item.label }}</div>
              <div class="truncate text-[11px] opacity-70">{{ item.description }}</div>
            </div>
          </button>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto px-3 pb-3">
        <div class="rounded-2xl border border-gray-200 bg-white/70 p-3 shadow-sm dark:border-white/10 dark:bg-white/5">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-[0.18em] text-gray-400">
                Shortcuts
              </div>
              <div class="mt-1 text-xs text-gray-500 dark:text-gray-400">常用功能直达</div>
            </div>
            <Sparkles class="h-4 w-4 text-amber-400" />
          </div>

          <div class="space-y-2">
            <button
              v-for="item in utilityNav"
              :key="item.path"
              class="flex w-full items-center gap-3 rounded-xl border border-transparent bg-gray-50 px-3 py-3 text-left transition-all hover:border-gray-200 hover:bg-white dark:bg-white/5 dark:hover:border-white/10 dark:hover:bg-white/10"
              @click="router.push(item.path)"
            >
              <div
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-white text-gray-600 shadow-sm dark:bg-white/10 dark:text-gray-200"
              >
                <component :is="item.icon" class="h-4 w-4" />
              </div>
              <div class="min-w-0 flex-1">
                <div class="truncate font-medium text-gray-800 dark:text-gray-100">{{ item.label }}</div>
                <div class="truncate text-xs text-gray-500 dark:text-gray-400">{{ item.description }}</div>
              </div>
              <ChevronRight class="h-4 w-4 text-gray-300 dark:text-gray-600" />
            </button>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 p-3 dark:border-white/10">
        <el-dropdown trigger="click" class="w-full">
          <button
            class="flex w-full items-center justify-between gap-3 rounded-2xl border border-gray-200 bg-white px-3 py-3 text-left shadow-sm outline-none transition-all hover:border-gray-300 hover:shadow-md dark:border-white/10 dark:bg-white/5 dark:hover:border-white/20"
          >
            <div class="flex min-w-0 items-center gap-3">
              <el-avatar :size="32" :src="avatarPreview">
                <User class="h-4 w-4" />
              </el-avatar>
              <div class="min-w-0">
                <div class="truncate font-medium text-gray-900 dark:text-white">
                  {{ userStore.user?.username || 'User' }}
                </div>
                <div class="truncate text-xs text-gray-500 dark:text-gray-400">
                  {{ routeLabel }}
                </div>
              </div>
            </div>
            <Settings class="h-4 w-4 shrink-0 text-gray-400 dark:text-gray-500" />
          </button>
          <template #dropdown>
            <el-dropdown-menu class="w-56">
              <el-dropdown-item @click="router.push('/dashboard')">Dashboard</el-dropdown-item>
              <el-dropdown-item @click="router.push('/settings')">Settings</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">Log out</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <main class="flex h-full min-w-0 flex-1 flex-col bg-white dark:bg-gray-800">
      <div
        class="flex h-14 items-center justify-between border-b border-gray-200 bg-white/80 px-5 backdrop-blur-sm dark:border-white/10 dark:bg-gray-800/80"
      >
        <div class="min-w-0">
          <div class="truncate text-sm font-semibold text-gray-800 dark:text-gray-100">{{ routeLabel }}</div>
          <div class="truncate text-xs text-gray-500 dark:text-gray-400">{{ routeDescription }}</div>
        </div>
        <div class="hidden items-center gap-2 md:flex">
          <button
            v-for="item in topActions"
            :key="item.path"
            class="flex items-center gap-2 rounded-full border border-gray-200 bg-white px-3 py-1.5 text-xs font-medium text-gray-600 transition-all hover:border-gray-300 hover:text-gray-900 dark:border-white/10 dark:bg-white/5 dark:text-gray-300 dark:hover:border-white/20 dark:hover:text-white"
            @click="router.push(item.path)"
          >
            <component :is="item.icon" class="h-3.5 w-3.5" />
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Bot,
  BrainCircuit,
  ChevronRight,
  Edit,
  FileStack,
  FolderKanban,
  Home,
  Library,
  Settings,
  Sparkles,
  User
} from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const primaryNav = [
  { path: '/dashboard', label: 'Dashboard', description: '总览知识资产与近期动态', icon: Home },
  { path: '/knowledge-base', label: 'Knowledge Base', description: '管理知识库与分类结构', icon: Library },
  { path: '/chat', label: 'AI Chat', description: '面向知识库进行连续问答', icon: Bot }
]

const utilityNav = [
  { path: '/documents', label: 'Document Workspace', description: '预览、编辑与总结文档资产', icon: FileStack },
  { path: '/document-tasks', label: 'Task Center', description: '查看处理状态、重试与取消任务', icon: FolderKanban },
  { path: '/retrieval-debug', label: 'Retrieval Debug', description: '核对改写与向量召回结果', icon: BrainCircuit }
]

const topActions = [
  { path: '/documents', label: 'Documents', icon: FileStack },
  { path: '/document-tasks', label: 'Tasks', icon: FolderKanban },
  { path: '/settings', label: 'Settings', icon: Settings }
]

const routeLabel = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': 'Dashboard',
    '/knowledge-base': 'Knowledge Base',
    '/chat': 'AI Chat',
    '/documents': 'Document Workspace',
    '/document-tasks': 'Task Center',
    '/retrieval-debug': 'Retrieval Debug',
    '/settings': 'Settings'
  }

  if (route.path.startsWith('/knowledge-base/')) {
    return 'Knowledge Base Detail'
  }

  return map[route.path] || 'AI Knowledge Base'
})

const routeDescription = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '查看当前知识库概览、入口和常用操作。',
    '/knowledge-base': '建立、整理和切换你的知识资产空间。',
    '/chat': '基于知识库上下文发起连续问答与追问。',
    '/documents': '管理解析文本、摘要资产与文档预览。',
    '/document-tasks': '集中查看上传、总结和重试任务。',
    '/retrieval-debug': '追踪 query rewrite 与检索命中链路。',
    '/settings': '配置账号资料、头像与模型参数。'
  }

  if (route.path.startsWith('/knowledge-base/')) {
    return '查看单个知识库的详情、分类与内容结构。'
  }

  return map[route.path] || '一个可持续使用的 AI 知识库工作台。'
})

const avatarPreview = computed(() => {
  const url = userStore.user?.avatarUrl
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/api/')) {
    return url
  }
  return `/api${url}`
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

async function handleLogout() {
  await userStore.logoutAction()
  router.push('/login')
}
</script>

<style>
.el-dropdown:focus,
.el-dropdown-link:focus,
.el-dropdown-trigger:focus {
  outline: none !important;
}
</style>
