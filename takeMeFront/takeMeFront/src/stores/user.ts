import { defineStore } from 'pinia'
import { getUserInfo, updateUserInfo, logout } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', {
  state: () => ({
    // 基础登录信息
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: '',
    role: Number(localStorage.getItem('role')) || 2, // 0管理员 1志愿者 2普通用户

    // 完整用户详情信息
    account: '',
    phone: '',
    age: null as number | null,
    gender: 0, // 0男 1女
    addresses: [] as string[], // 常用地址列表（最多3个）
    emergencyName: '', // 紧急联系人姓名
    emergencyPhone: '', // 紧急联系人电话
    avatar: ''
  }),

  actions: {
    // 登录存入基础信息
    setUserInfo(token: string, userId: string, username: string, role: number) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = role

      localStorage.setItem('token', token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('role', String(role))
    },

    // 从后端获取完整用户详情
    async getUserInfo() {
      try {
        const res = await getUserInfo()
        this.$patch(res.data)
        return true
      } catch (err) {
        ElMessage.error('获取用户信息失败')
        return false
      }
    },

    // 更新用户信息
    async updateUserInfo(data: any) {
      try {
        await updateUserInfo(data)
        this.$patch(data)
        ElMessage.success('信息修改成功')
        return true
      } catch (err) {
        ElMessage.error('修改失败')
        return false
      }
    },

    // 退出登录清空所有信息
    async logout() {
      try {
        await logout()
      } finally {
        localStorage.clear()
        this.$reset()
        ElMessage.success('已退出登录')
      }
    }
  }
})
