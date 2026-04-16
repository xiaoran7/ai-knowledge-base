import { get, post, put, del } from './index'

// LLM 提供商数据
export interface LlmProvider {
  provider: string
  name: string
  defaultBaseUrl: string
  models: string[]
  defaultEmbeddingModel: string
}

// LLM 配置数据
export interface LlmConfig {
  id: string
  provider: string
  providerName: string
  apiKey: string
  apiBaseUrl: string
  modelName: string
  embeddingModel: string
  isDefault: boolean
  isEnabled: boolean
  temperature: number
  maxTokens: number
  remark: string
  createdAt: string
}

// 配置请求参数
export interface LlmConfigRequest {
  provider: string
  apiKey: string
  apiBaseUrl?: string
  modelName?: string
  embeddingModel?: string
  isDefault?: boolean
  temperature?: number
  maxTokens?: number
  remark?: string
}

/**
 * 获取支持的 LLM 提供商列表
 */
export function getLlmProviders() {
  return get<LlmProvider[]>('/llm-config/providers')
}

/**
 * 获取用户的所有 LLM 配置
 */
export function getLlmConfigs() {
  return get<LlmConfig[]>('/llm-config')
}

/**
 * 获取用户的默认配置
 */
export function getDefaultLlmConfig() {
  return get<LlmConfig>('/llm-config/default')
}

/**
 * 创建 LLM 配置
 */
export function createLlmConfig(data: LlmConfigRequest) {
  return post<LlmConfig>('/llm-config', data)
}

/**
 * 更新 LLM 配置
 */
export function updateLlmConfig(id: string, data: LlmConfigRequest) {
  return put<LlmConfig>(`/llm-config/${id}`, data)
}

/**
 * 设置为默认配置
 */
export function setDefaultLlmConfig(id: string) {
  return put(`/llm-config/${id}/default`)
}

/**
 * 启用/禁用配置
 */
export function setLlmConfigEnabled(id: string, enabled: boolean) {
  return put(`/llm-config/${id}/enabled?enabled=${enabled}`)
}

/**
 * 删除配置
 */
export function deleteLlmConfig(id: string) {
  return del(`/llm-config/${id}`)
}