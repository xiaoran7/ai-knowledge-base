import type { AxiosRequestConfig } from 'axios'
import { del, get, post, put, request } from './index'

export interface Source {
  documentId: string
  documentTitle: string
  content: string
  score: number
}

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  thinking?: string
  sources?: Source[]
  createdAt: string
}

export interface Conversation {
  id: string
  title: string
  messageCount: number
  lastMessageAt: string
}

export interface ConversationDetail extends Conversation {
  sessionSummary?: string
  sessionFacts?: string
  messages: Message[]
}

export interface ChatParams {
  knowledgeBaseId: string
  message: string
  conversationId?: string
}

export interface ChatResponse {
  conversationId: string
  messageId: string
  title?: string
  content: string
  thinking?: string
  sources: Source[]
}

export interface RetrievalDebugHit {
  chunkId: string
  documentId: string
  documentTitle: string
  content: string
  chunkType: string
  chunkIndex: string
  score: number
}

export interface RetrievalDebugResponse {
  originalQuery: string
  rewrittenQuery: string
  usedQuery: string
  originalHits: RetrievalDebugHit[]
  rewrittenHits: RetrievalDebugHit[]
  finalHits: RetrievalDebugHit[]
}

export interface ConversationMemoryUpdateRequest {
  sessionSummary?: string
  sessionFacts?: string
}

export function chat(data: ChatParams, config?: AxiosRequestConfig) {
  return post<ChatResponse>('/ai/chat', data, config)
}

export function debugRetrieval(data: { knowledgeBaseId: string; message: string; topK?: number }) {
  return post<RetrievalDebugResponse>('/ai/retrieval-debug', data)
}

export function getConversationList(params: {
  knowledgeBaseId: string
  page?: number
  size?: number
}) {
  return get<Conversation[]>('/conversations', params)
}

export function getConversationDetail(id: string) {
  return get<ConversationDetail>(`/conversations/${id}`)
}

export function updateConversationMemory(id: string, data: ConversationMemoryUpdateRequest) {
  return put<ConversationDetail>(`/conversations/${id}/memory`, data)
}

export function deleteConversation(id: string) {
  return del(`/conversations/${id}`)
}

export async function exportConversation(id: string, format: 'markdown' | 'pdf') {
  const blob = await request.get(`/conversations/${id}/export`, {
    params: { format },
    responseType: 'blob'
  })
  return blob as unknown as Blob
}
