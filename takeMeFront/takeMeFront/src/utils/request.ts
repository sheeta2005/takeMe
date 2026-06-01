import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 全局唯一标记：是否正在执行登出流程，防止多个并发请求同时触发登出
let isLoggingOut = false

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

// 请求拦截：统一带Authorization头（Bearer Token格式）
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (!isLoggingOut && userStore.token) {
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截：修复401死循环核心逻辑
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(res)
    }
    return res
  },
  (error) => {
    const { response } = error || {}
    const status = response?.status

    if (status === 401 && !isLoggingOut) {
      isLoggingOut = true
      ElMessage.error('登录已过期，请重新登录')
      const userStore = useUserStore()
      userStore.logout()
      setTimeout(() => {
        isLoggingOut = false
      }, 1000)
    } else if (status !== 401 && !isLoggingOut) {
      ElMessage.error('网络或服务器异常')
    }
    return Promise.reject(error)
  }
)

export default request
