import { get, post, del, put } from './index'

// 知识库数据
export interface KnowledgeBase {
  id: string
  name: string
  description: string
  documentCount: number
  createdAt: string
}

// 分类数据
export interface Category {
  id: string
  name: string
  parentId: string | null
  documentCount: number
}

// 分类树数据
export interface CategoryTree {
  id: string
  name: string
  documentCount: number
  children: CategoryTree[]
}

export interface KnowledgeGraphNode {
  id: string
  type: 'DOCUMENT' | 'CATEGORY'
  title: string
  categoryId?: string | null
  categoryName?: string | null
  status?: string | null
  summaryType?: string | null
  tags: string[]
  degree: number
  inbound: number
  outbound: number
  virtualNode: boolean
}

export interface KnowledgeGraphEdge {
  id: string
  source: string
  target: string
  type: 'REFERENCE' | 'SHARED_TAG' | 'CATEGORY_MEMBERSHIP' | 'CATEGORY_TREE' | string
  label?: string
  weight: number
}

export interface KnowledgeGraphStats {
  totalNodes: number
  totalEdges: number
  documentNodes: number
  categoryNodes: number
  referenceEdges: number
  sharedTagEdges: number
  membershipEdges: number
  orphanDocuments: number
}

export interface KnowledgeGraphResponse {
  knowledgeBaseId: string
  knowledgeBaseName: string
  nodes: KnowledgeGraphNode[]
  edges: KnowledgeGraphEdge[]
  stats: KnowledgeGraphStats
}

/**
 * 获取知识库列表
 */
export function getKnowledgeBaseList() {
  return get<KnowledgeBase[]>('/knowledge-base')
}

/**
 * 创建知识库
 */
export function createKnowledgeBase(data: { name: string; description: string }) {
  return post<KnowledgeBase>('/knowledge-base', data)
}

/**
 * 删除知识库
 */
export function deleteKnowledgeBase(id: string) {
  return del(`/knowledge-base/${id}`)
}

export function getKnowledgeGraph(kbId: string) {
  return get<KnowledgeGraphResponse>(`/knowledge-base/${kbId}/graph`)
}

/**
 * 获取分类列表（扁平）
 */
export function getCategoryList(kbId: string) {
  return get<Category[]>(`/knowledge-base/${kbId}/categories`)
}

/**
 * 获取分类树（层级结构）
 */
export function getCategoryTree(kbId: string) {
  return get<CategoryTree[]>(`/knowledge-base/${kbId}/categories/tree`)
}

/**
 * 创建分类
 */
export function createCategory(kbId: string, data: { name: string; parentId?: string | null }) {
  return post<Category>(`/knowledge-base/${kbId}/categories`, data)
}

/**
 * 更新分类名称
 */
export function updateCategory(categoryId: string, name: string) {
  return put<Category>(`/knowledge-base/categories/${categoryId}`, { name })
}

/**
 * 删除分类
 */
export function deleteCategory(id: string) {
  return del(`/knowledge-base/categories/${id}`)
}
