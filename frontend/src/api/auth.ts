import { get, post, put, upload } from './index'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

export interface RegisterParams {
  username: string
  email: string
  password: string
}

export interface UserInfo {
  userId: string
  username: string
  email: string
  avatarUrl?: string
  assistantAvatarUrl?: string
  bio?: string
  role?: string
  createdAt?: string
}

export interface UpdateUserProfileParams {
  avatarUrl?: string
  assistantAvatarUrl?: string
  bio?: string
}

export function login(data: LoginParams) {
  return post<LoginResponse>('/auth/login', data)
}

export function register(data: RegisterParams) {
  return post('/auth/register', data)
}

export function logout() {
  return post('/auth/logout')
}

export function refreshToken(refreshToken: string) {
  return post<{ accessToken: string; expiresIn: number }>('/auth/refresh', { refreshToken })
}

export function getCurrentUser() {
  return get<UserInfo>('/user/me')
}

export function updateCurrentUser(data: UpdateUserProfileParams) {
  return put<UserInfo>('/user/me', data)
}

export function uploadCurrentUserAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return upload<UserInfo>('/user/me/avatar', formData)
}

export function uploadCurrentAssistantAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return upload<UserInfo>('/user/me/assistant-avatar', formData)
}
