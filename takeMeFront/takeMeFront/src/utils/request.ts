// src/utils/request.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useVolunteerStore } from '@/stores/volunteer'

// 防止并发请求重复触发登出
let isLoggingOut = false

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

// 请求拦截：自动匹配身份 Token
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  const volunteerStore = useVolunteerStore()

  if (!isLoggingOut) {
    // 志愿者接口 → 携带志愿者 Token
    if (config.url?.startsWith('/api/volunteer')) {
      if (volunteerStore.token) {
        config.headers.Authorization = `Bearer ${volunteerStore.token}`
      }
    }
    // 用户接口 → 携带用户 Token
    else if (config.url?.startsWith('/api/user')) {
      if (userStore.token) {
        config.headers.Authorization = `Bearer ${userStore.token}`
      }
    }
  }
  return config
})

// 响应拦截：精准处理 401，不牵连其他身份
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(res)
    }
    return res
  },
  async (error) => {
    const { response, config } = error || {}
    const status = response?.status
    const reqUrl = config?.url || ''

    // 仅 401 登录过期 执行登出，且只清理对应身份
    if (status === 401 && !isLoggingOut) {
      isLoggingOut = true
      ElMessage.error('登录已过期，请重新登录')

      const userStore = useUserStore()
      const volunteerStore = useVolunteerStore()

      // 志愿者接口 401 → 只清志愿者
      if (reqUrl.startsWith('/api/volunteer')) {
        volunteerStore.$reset()
        localStorage.removeItem('volunteerToken')
        localStorage.removeItem('volunteerId')
        localStorage.removeItem('volunteerUsername')
        localStorage.removeItem('volunteerRole')
      }
      // 用户接口 401 → 只清用户
      else if (reqUrl.startsWith('/api/user')) {
        userStore.$reset()
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('role')
      }

      setTimeout(() => {
        isLoggingOut = false
        window.location.href = '/login'
      }, 500)
    }
    // 404 / 500 / 403 等错误 → 只提示，不登出
    else if (status !== 401 && !isLoggingOut) {
      const msg = response?.data?.msg || '服务器异常'
      ElMessage.error(msg)
    }

    return Promise.reject(error)
  }
)

export default request
