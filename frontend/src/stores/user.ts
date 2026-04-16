import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getCurrentUser,
  login,
  logout,
  updateCurrentUser,
  uploadCurrentAssistantAvatar,
  uploadCurrentUserAvatar,
  type UpdateUserProfileParams,
  type UserInfo
} from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('access_token') || '')
  const user = ref<UserInfo | null>(null)
  const hydrating = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => user.value?.username || '未登录用户')

  async function loginAction(username: string, password: string) {
    const res = await login({ username, password })
    token.value = res.accessToken
    user.value = res.user

    localStorage.setItem('access_token', res.accessToken)
    localStorage.setItem('refresh_token', res.refreshToken)
    localStorage.setItem('user_profile', JSON.stringify(res.user))

    return res
  }

  async function hydrateUser() {
    if (!token.value || hydrating.value) {
      return user.value
    }

    hydrating.value = true
    try {
      if (!user.value) {
        const cached = localStorage.getItem('user_profile')
        if (cached) {
          user.value = JSON.parse(cached) as UserInfo
        }
      }

      const currentUser = await getCurrentUser()
      user.value = currentUser
      localStorage.setItem('user_profile', JSON.stringify(currentUser))
      return currentUser
    } catch (error) {
      await logoutAction()
      throw error
    } finally {
      hydrating.value = false
    }
  }

  async function updateProfileAction(payload: UpdateUserProfileParams) {
    const currentUser = await updateCurrentUser(payload)
    user.value = currentUser
    localStorage.setItem('user_profile', JSON.stringify(currentUser))
    return currentUser
  }

  async function uploadAvatarAction(file: File) {
    const currentUser = await uploadCurrentUserAvatar(file)
    user.value = currentUser
    localStorage.setItem('user_profile', JSON.stringify(currentUser))
    return currentUser
  }

  async function uploadAssistantAvatarAction(file: File) {
    const currentUser = await uploadCurrentAssistantAvatar(file)
    user.value = currentUser
    localStorage.setItem('user_profile', JSON.stringify(currentUser))
    return currentUser
  }

  async function logoutAction() {
    try {
      await logout()
    } catch {
      // ignore logout API failures
    }

    token.value = ''
    user.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user_profile')
  }

  return {
    token,
    user,
    isLoggedIn,
    username,
    hydrating,
    loginAction,
    hydrateUser,
    updateProfileAction,
    uploadAvatarAction,
    uploadAssistantAvatarAction,
    logoutAction
  }
})
