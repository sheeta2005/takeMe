import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: '',
    role: 2 // 默认普通用户 0管理员 1志愿者 2普通
  }),
  actions: {
    // 登录存入信息
    setUserInfo(token: string, username: string, role: number) {
      this.token = token
      this.username = username
      this.role = role
      localStorage.setItem('token', token)
    },
    // 退出登录清空
    logout() {
      this.token = ''
      this.username = ''
      this.role = 2
      localStorage.removeItem('token')
    }
  }
})
