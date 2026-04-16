import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Category, KnowledgeBase } from '@/api/knowledge'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  getCategoryList,
  getKnowledgeBaseList
} from '@/api/knowledge'

const STORAGE_KEY = 'current_kb_id'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeBaseList = ref<KnowledgeBase[]>([])
  const currentKbId = ref<string>(localStorage.getItem(STORAGE_KEY) || '')
  const categoryList = ref<Category[]>([])

  const currentKb = computed(() =>
    knowledgeBaseList.value.find((kb) => kb.id === currentKbId.value) || null
  )

  async function fetchKnowledgeBaseList() {
    const res = await getKnowledgeBaseList()
    knowledgeBaseList.value = res

    if (!currentKbId.value && knowledgeBaseList.value.length > 0) {
      setCurrentKb(knowledgeBaseList.value[0].id)
    } else if (
      currentKbId.value &&
      !knowledgeBaseList.value.some((kb) => kb.id === currentKbId.value)
    ) {
      setCurrentKb(knowledgeBaseList.value[0]?.id || '')
    }

    return knowledgeBaseList.value
  }

  async function createKb(name: string, description: string) {
    const res = await createKnowledgeBase({ name, description })
    knowledgeBaseList.value.push(res)
    if (!currentKbId.value) {
      setCurrentKb(res.id)
    }
    return res
  }

  async function deleteKb(id: string) {
    await deleteKnowledgeBase(id)
    knowledgeBaseList.value = knowledgeBaseList.value.filter((kb) => kb.id !== id)
    if (currentKbId.value === id) {
      setCurrentKb(knowledgeBaseList.value[0]?.id || '')
    }
  }

  async function fetchCategoryList() {
    if (!currentKbId.value) {
      categoryList.value = []
      return []
    }

    const res = await getCategoryList(currentKbId.value)
    categoryList.value = res
    return categoryList.value
  }

  function setCurrentKb(kbId: string) {
    currentKbId.value = kbId
    if (kbId) {
      localStorage.setItem(STORAGE_KEY, kbId)
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  return {
    knowledgeBaseList,
    currentKbId,
    currentKb,
    categoryList,
    fetchKnowledgeBaseList,
    createKb,
    deleteKb,
    fetchCategoryList,
    setCurrentKb
  }
})
