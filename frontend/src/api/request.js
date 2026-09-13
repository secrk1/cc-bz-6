import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const service = axios.create({
  baseURL: '/',
  timeout: 10000
})

// 请求拦截器：自动携带认证令牌
service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理后端 Result 结构与异常
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非标准结构（如文件流）直接返回
    if (res === null || typeof res !== 'object' || res.code === undefined) {
      return res
    }
    if (res.code === 0) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    const status = error.response?.status
    const resData = error.response?.data

    if (status === 401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      ElMessage.error(resData?.message || '登录已失效，请重新登录')
      router.replace({
        path: '/login',
        query: router.currentRoute.value.fullPath !== '/login'
          ? { redirect: router.currentRoute.value.fullPath }
          : undefined
      })
    } else {
      ElMessage.error(resData?.message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
