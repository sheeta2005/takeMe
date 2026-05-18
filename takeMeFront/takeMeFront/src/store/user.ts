import { defineStore } from 'pinia'

interface UserState {
  token: string
  role: string
  userName: string
  userId: number
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    userName: '',
    userId: 0
  }),
  actions: {
    setToken(token: string, role: string, userName: string = '', userId: number = 0) {
      this.token = token
      this.role = role
      this.userName = userName
      this.userId = userId
      localStorage.setItem('token', token)
      localStorage.setItem('role', role)
    },
    logout() {
      this.token = ''
      this.role = ''
      this.userName = ''
      this.userId = 0
      localStorage.removeItem('token')
      localStorage.removeItem('role')
    }
  }
})
