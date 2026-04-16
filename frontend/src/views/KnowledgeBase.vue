<template>
  <div class="knowledge-base page-container">
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新建知识库
      </el-button>
    </div>

    <el-card v-loading="loading">
      <el-empty v-if="knowledgeBaseList.length === 0" description="暂无知识库，点击上方按钮创建" />

      <el-table v-else :data="knowledgeBaseList" style="width: 100%">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="documentCount" label="文档数" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEnter(row)">
              进入
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建知识库对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建知识库" width="400px">
      <el-form ref="formRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" placeholder="请输入描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import type { KnowledgeBase } from '@/api/knowledge'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const loading = ref(false)
// 直接使用 store 的列表
const knowledgeBaseList = computed(() => knowledgeStore.knowledgeBaseList)

// 创建对话框
const createDialogVisible = ref(false)
const createLoading = ref(false)
const formRef = ref<FormInstance>()
const createForm = reactive({
  name: '',
  description: ''
})
const createRules: FormRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度 2-50 位', trigger: 'blur' }
  ]
}

onMounted(async () => {
  loading.value = true
  try {
    await knowledgeStore.fetchKnowledgeBaseList()
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
})

const showCreateDialog = () => {
  createForm.name = ''
  createForm.description = ''
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return // 验证失败
  }

  createLoading.value = true
  try {
    await knowledgeStore.createKb(createForm.name, createForm.description)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
  } catch {
    // 错误已由拦截器处理
  } finally {
    createLoading.value = false
  }
}

const handleEnter = (row: KnowledgeBase) => {
  knowledgeStore.setCurrentKb(row.id)
  router.push(`/knowledge-base/${row.id}`)
}

const handleDelete = (row: KnowledgeBase) => {
  ElMessageBox.confirm(`确定删除知识库 "${row.name}" 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await knowledgeStore.deleteKb(row.id)
      ElMessage.success('删除成功')
    } catch {
      // 错误已由拦截器处理
    }
  })
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}
</style>