<template>
  <div class="kb-detail page-container">
    <el-card class="kb-info-card">
      <div class="kb-header">
        <div class="kb-title">
          <h2>{{ kbInfo?.name }}</h2>
          <el-tag type="info">{{ kbInfo?.documentCount || 0 }} 个文档</el-tag>
        </div>
        <div class="kb-actions">
          <el-button type="primary" @click="goToChat">
            <el-icon><ChatDotRound /></el-icon>
            AI 问答
          </el-button>
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </div>
      <p class="kb-desc">{{ kbInfo?.description || '暂无描述' }}</p>
    </el-card>

    <el-tabs v-model="activeTab" class="mt-4">
      <el-tab-pane label="文档管理" name="documents">
        <div class="tab-header">
          <el-button type="primary" @click="showUploadDialog">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
          <el-button @click="showCategoryDialog">
            <el-icon><FolderAdd /></el-icon>
            新建分类
          </el-button>
        </div>

        <el-form :inline="true" class="filter-form">
          <el-form-item label="分类">
            <el-tree-select
              v-model="selectedCategoryId"
              :data="categoryTreeData"
              placeholder="全部分类"
              clearable
              check-strictly
              :render-after-expand="false"
            />
          </el-form-item>
        </el-form>

        <el-table v-loading="docLoading" :data="documentList" style="width: 100%">
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="categoryName" label="分类" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.categoryName" size="small">{{ row.categoryName }}</el-tag>
              <span v-else class="text-gray">未分类</span>
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="类型" width="90" />
          <el-table-column prop="fileSize" label="大小" width="110">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showEditDocCategory(row)">
                设置分类
              </el-button>
              <el-button link type="danger" @click="handleDeleteDoc(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="docPage"
            v-model:page-size="docSize"
            :total="docTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadDocuments"
            @current-change="loadDocuments"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="分类管理" name="categories">
        <div class="tab-header">
          <el-button type="primary" @click="showCategoryDialog">
            <el-icon><FolderAdd /></el-icon>
            新建分类
          </el-button>
        </div>

        <el-table
          :data="categoryTree"
          row-key="id"
          default-expand-all
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          style="width: 100%"
        >
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="documentCount" label="文档数" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" @click="showEditCategoryDialog(row)">
                编辑
              </el-button>
              <el-button link type="primary" @click="showCreateSubCategory(row)">
                添加子分类
              </el-button>
              <el-button link type="danger" @click="handleDeleteCategory(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="uploadDialogVisible" title="上传文档" width="500px">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="选择文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleFileChange">
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word、Excel、Markdown、TXT 等格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="uploadForm.categoryId"
            :data="categoryTreeData"
            placeholder="选择分类（可选）"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" :title="editCategoryId ? '编辑分类' : '新建分类'" width="400px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item v-if="!editCategoryId" label="父分类">
          <el-tree-select
            v-model="categoryForm.parentId"
            :data="categoryTreeData"
            placeholder="选择父分类（可选）"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categoryLoading" @click="handleCategorySubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDocCategoryVisible" title="设置文档分类" width="400px">
      <el-form label-width="80px">
        <el-form-item label="文档">
          <span>{{ editDocCategoryDoc?.title }}</span>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="editDocCategoryCategoryId"
            :data="categoryTreeData"
            placeholder="选择分类"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDocCategoryVisible = false">取消</el-button>
        <el-button type="primary" :loading="editDocCategoryLoading" @click="handleSetDocCategory">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ChatDotRound, Back, Upload, FolderAdd } from '@element-plus/icons-vue'
import { useKnowledgeStore } from '@/stores/knowledge'
import {
  createCategory,
  deleteCategory,
  getCategoryTree,
  getKnowledgeBaseList,
  updateCategory,
  type CategoryTree,
  type KnowledgeBase
} from '@/api/knowledge'
import {
  deleteDocument,
  getDocumentList,
  setDocumentCategory,
  uploadDocument,
  type Document
} from '@/api/document'

type TreeSelectNode = {
  value: string
  label: string
  children: TreeSelectNode[]
}

const route = useRoute()
const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const kbId = computed(() => route.params.id as string)
const kbInfo = ref<KnowledgeBase | null>(null)
const activeTab = ref('documents')

const docLoading = ref(false)
const documentList = ref<Document[]>([])
const selectedCategoryId = ref('')
const docPage = ref(1)
const docSize = ref(10)
const docTotal = ref(0)

const categoryTree = ref<CategoryTree[]>([])
const categoryTreeData = computed<TreeSelectNode[]>(() =>
  categoryTree.value.map((cat) => toTreeSelectData(cat))
)

const uploadDialogVisible = ref(false)
const uploadForm = reactive({ categoryId: '' })
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

const categoryDialogVisible = ref(false)
const categoryFormRef = ref<FormInstance>()
const categoryForm = reactive({ name: '', parentId: '' })
const categoryRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}
const categoryLoading = ref(false)
const editCategoryId = ref('')

const editDocCategoryVisible = ref(false)
const editDocCategoryDoc = ref<Document | null>(null)
const editDocCategoryCategoryId = ref('')
const editDocCategoryLoading = ref(false)

onMounted(async () => {
  knowledgeStore.setCurrentKb(kbId.value)
  await loadKbInfo()
  await loadCategories()
  await loadDocuments()
})

watch(selectedCategoryId, async () => {
  docPage.value = 1
  await loadDocuments()
})

async function loadKbInfo() {
  const list = await getKnowledgeBaseList()
  kbInfo.value = list.find((kb) => kb.id === kbId.value) || null
  if (!kbInfo.value) {
    ElMessage.error('知识库不存在')
    router.push('/knowledge-base')
  }
}

async function loadCategories() {
  categoryTree.value = await getCategoryTree(kbId.value)
}

async function loadDocuments() {
  docLoading.value = true
  try {
    const res = await getDocumentList({
      knowledgeBaseId: kbId.value,
      categoryId: selectedCategoryId.value || undefined,
      page: docPage.value,
      size: docSize.value
    })
    documentList.value = res.list
    docTotal.value = res.total
  } finally {
    docLoading.value = false
  }
}

function showUploadDialog() {
  uploadForm.categoryId = ''
  selectedFile.value = null
  uploadDialogVisible.value = true
}

function handleFileChange(file: UploadFile) {
  selectedFile.value = (file.raw as File) || null
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(selectedFile.value, {
      knowledgeBaseId: kbId.value,
      categoryId: uploadForm.categoryId || undefined
    })
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    await Promise.all([loadDocuments(), loadKbInfo(), loadCategories()])
  } finally {
    uploading.value = false
  }
}

function handleDeleteDoc(row: Document) {
  ElMessageBox.confirm(`确定删除文档“${row.title}”吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    await Promise.all([loadDocuments(), loadKbInfo(), loadCategories()])
  })
}

function showCategoryDialog() {
  editCategoryId.value = ''
  categoryForm.name = ''
  categoryForm.parentId = ''
  categoryDialogVisible.value = true
}

function showCreateSubCategory(parent: CategoryTree) {
  editCategoryId.value = ''
  categoryForm.name = ''
  categoryForm.parentId = parent.id
  categoryDialogVisible.value = true
}

function showEditCategoryDialog(category: CategoryTree) {
  editCategoryId.value = category.id
  categoryForm.name = category.name
  categoryForm.parentId = ''
  categoryDialogVisible.value = true
}

async function handleCategorySubmit() {
  if (!categoryFormRef.value) return
  await categoryFormRef.value.validate()

  categoryLoading.value = true
  try {
    if (editCategoryId.value) {
      await updateCategory(editCategoryId.value, categoryForm.name)
      ElMessage.success('更新成功')
    } else {
      await createCategory(kbId.value, {
        name: categoryForm.name,
        parentId: categoryForm.parentId || null
      })
      ElMessage.success('创建成功')
    }
    categoryDialogVisible.value = false
    await loadCategories()
  } finally {
    categoryLoading.value = false
  }
}

function handleDeleteCategory(row: CategoryTree) {
  ElMessageBox.confirm(
    `确定删除分类“${row.name}”吗？删除后，该分类下的文档会变成未分类。`,
    '提示',
    { type: 'warning' }
  ).then(async () => {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    await Promise.all([loadCategories(), loadDocuments()])
  })
}

function showEditDocCategory(doc: Document) {
  editDocCategoryDoc.value = doc
  editDocCategoryCategoryId.value = doc.categoryId || ''
  editDocCategoryVisible.value = true
}

async function handleSetDocCategory() {
  if (!editDocCategoryDoc.value) {
    ElMessage.warning('文档信息不正确')
    return
  }

  editDocCategoryLoading.value = true
  try {
    await setDocumentCategory(editDocCategoryDoc.value.id, editDocCategoryCategoryId.value || null)
    ElMessage.success('分类设置成功')
    editDocCategoryVisible.value = false
    await Promise.all([loadDocuments(), loadCategories()])
  } finally {
    editDocCategoryLoading.value = false
  }
}

function goToChat() {
  knowledgeStore.setCurrentKb(kbId.value)
  router.push('/chat')
}

function goBack() {
  router.push('/knowledge-base')
}

function toTreeSelectData(cat: CategoryTree): TreeSelectNode {
  return {
    value: cat.id,
    label: cat.name,
    children: cat.children?.map((child) => toTreeSelectData(child)) || []
  }
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function getStatusType(status: string) {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    pending: 'warning',
    processed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '处理中',
    processed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.kb-info-card {
  margin-bottom: 20px;
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-title h2 {
  margin: 0;
}

.kb-actions {
  display: flex;
  gap: 12px;
}

.kb-desc {
  margin-top: 12px;
  color: #666;
}

.mt-4 {
  margin-top: 20px;
}

.tab-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-form {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.text-gray {
  color: #999;
}
</style>
