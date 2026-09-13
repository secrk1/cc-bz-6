import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo } from '@/api/auth'

const TOKEN_KEY = 'workflow_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.user?.nickname || state.user?.username || '',
    role: (state) => state.user?.role || ''
  },

  actions: {
    /** 登录：校验通过后保存令牌与用户信息 */
    async login(loginForm) {
      const data = await loginApi(loginForm)
      this.token = data.token
      this.user = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
      return data
    },

    /** 拉取当前用户信息（用于路由守卫校验令牌有效性） */
    async fetchUserInfo() {
      const user = await getUserInfo()
      this.user = user
      return user
    },

    /** 清除本地认证状态 */
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
