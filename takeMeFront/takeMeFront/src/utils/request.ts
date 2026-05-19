import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

// 请求拦截
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['token'] = userStore.token
  }
  return config
})

// 响应拦截
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 成功
    if (res.code === 200) {
      return res
    }

    // token 过期
    if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      const userStore = useUserStore()
      userStore.logout()
      setTimeout(() => {
        window.location.href = '/login'
      }, 800)
    }

    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
    ElMessage.error('网络或服务器异常')
    return Promise.reject(error)
  }
)

export default request
