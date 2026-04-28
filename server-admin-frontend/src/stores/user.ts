import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo, type LoginParams, type UserInfo } from '@/api/auth'
import router from '@/router'

const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  /** 登录 */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.token
    userInfo.value = res.userInfo
    localStorage.setItem('token', res.token)
    return res
  }

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出 API 错误
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    router.push('/login')
  }

  /** 获取用户信息 */
  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res
    return res
  }

  /** 获取 token */
  function getToken(): string {
    return token.value
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
    getToken,
  }
})

export default useUserStore
