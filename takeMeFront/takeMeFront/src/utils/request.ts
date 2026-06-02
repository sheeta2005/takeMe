import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useVolunteerStore } from '@/stores/volunteer'
import { useAdminStore } from '@/stores/admin'

let isLoggingOut = false

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  const volunteerStore = useVolunteerStore()
  const adminStore = useAdminStore()

  if (!isLoggingOut) {
    if (config.url?.startsWith('/api/admin')) {
      if (adminStore.token) {
        config.headers.Authorization = `Bearer ${adminStore.token}`
      }
    }
    else if (config.url?.startsWith('/api/volunteer')) {
      if (volunteerStore.token) {
        config.headers.Authorization = `Bearer ${volunteerStore.token}`
      }
    }
    else if (config.url?.startsWith('/api/user')) {
      if (userStore.token) {
        config.headers.Authorization = `Bearer ${userStore.token}`
      }
    }
  }
  return config
})

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

    if (status === 401 && !isLoggingOut) {
      isLoggingOut = true
      ElMessage.error('登录已过期，请重新登录')

      const userStore = useUserStore()
      const volunteerStore = useVolunteerStore()
      const adminStore = useAdminStore()

      if (reqUrl.startsWith('/api/admin')) {
        adminStore.$reset()
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminId')
      }
      else if (reqUrl.startsWith('/api/volunteer')) {
        volunteerStore.$reset()
        localStorage.removeItem('volunteerToken')
        localStorage.removeItem('volunteerId')
        localStorage.removeItem('volunteerUsername')
        localStorage.removeItem('volunteerRole')
      }
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
    else if (status !== 401 && !isLoggingOut) {
      const msg = response?.data?.msg || '服务器异常'
      ElMessage.error(msg)
    }

    return Promise.reject(error)
  }
)

export default request
