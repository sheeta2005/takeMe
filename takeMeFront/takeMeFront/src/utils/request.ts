import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080', // 关键：后端端口8080
  timeout: 5000
})

// 请求拦截器（可选）
request.interceptors.request.use(config => {
  return config
})

// 响应拦截器（可选，统一处理返回值）
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  error => {
    return Promise.reject(error)
  }
)

export default request
