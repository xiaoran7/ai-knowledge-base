import { del, get, post, put, upload } from './index'

export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'SUMMARIZED' | 'FAILED' | 'CANCELED'

export interface Document {
  id: string
  title: string
  fileType: string
  fileSize: number
  status: DocumentStatus
  processingStage?: 'UPLOADED' | 'PARSING' | 'SUMMARIZING' | 'INDEXING' | 'COMPLETED' | 'FAILED'
  createdAt: string
  categoryId?: string
  categoryName?: string
  tags?: string[]
  summaryContent?: string
  summaryType?: string
  summaryUpdatedAt?: string
  lastError?: string
}

export interface DocumentDetail extends Document {
  content?: string
}

export interface DocumentTask {
  id: string
  documentId: string
  documentTitle: string
  taskType: string
  status: string
  processingStage?: string
  summaryMode?: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
  completedAt?: string
}

export interface DocumentTaskListResponse {
  list: DocumentTask[]
}

export interface DocumentListResponse {
  list: Document[]
  total: number
  page: number
  size: number
}

export interface UploadParams {
  knowledgeBaseId: string
  categoryId?: string
  title?: string
}

export interface DocumentUpdateRequest {
  content?: string
  summaryContent?: string
  summaryMode?: string
  regenerateSummary?: boolean
  tags?: string[]
}

export interface DocumentSummaryRequest {
  content?: string
  summaryMode?: string
}

export function uploadDocument(file: File, params: UploadParams) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('knowledgeBaseId', params.knowledgeBaseId)
  if (params.categoryId) {
    formData.append('categoryId', params.categoryId)
  }
  if (params.title) {
    formData.append('title', params.title)
  }
  return upload<{ documentId: string; title: string; status: DocumentStatus; summaryContent?: string; summaryType?: string }>(
    '/documents/upload',
    formData
  )
}

export function getDocumentList(params: {
  knowledgeBaseId: string
  categoryId?: string
  page?: number
  size?: number
}) {
  return get<DocumentListResponse>('/documents', params)
}

export function getDocumentDetail(id: string) {
  return get<DocumentDetail>(`/documents/${id}`)
}

export function getDocumentTaskList(params: {
  knowledgeBaseId: string
  documentId?: string
}) {
  return get<DocumentTaskListResponse>('/documents/tasks', params)
}

export function cancelDocumentTask(taskId: string) {
  return post<DocumentTask>(`/documents/tasks/${taskId}/cancel`)
}

export function updateDocument(id: string, data: DocumentUpdateRequest) {
  return put<DocumentDetail>(`/documents/${id}`, data)
}

export function generateDocumentSummary(id: string, data: DocumentSummaryRequest) {
  return post<DocumentDetail>(`/documents/${id}/summary`, data)
}

export function retryDocumentProcessing(id: string) {
  return post<DocumentDetail>(`/documents/${id}/retry`)
}

export function deleteDocument(id: string) {
  return del(`/documents/${id}`)
}

export function setDocumentCategory(id: string, categoryId: string | null) {
  const url = `/documents/${id}/category`
  const params = new URLSearchParams()
  if (categoryId) {
    params.append('categoryId', categoryId)
  }
  return put(url + (params.toString() ? `?${params.toString()}` : ''))
}
