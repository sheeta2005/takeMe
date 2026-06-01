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
    realName: '',
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
      // ✅ 没有token直接返回，不发起请求
      if (!this.token) return false

      try {
        const res = await getUserInfo()
        this.$patch(res.data)
        return true
      } catch (err) {
        // 401错误已经在响应拦截器处理了，这里不用再弹
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

    // 退出登录
    async logout() {
      try {
        // 先调用logout接口（此时token还在）
        await logout()
      } catch (err) {
        // 接口失败也继续清状态
      } finally {
        // ✅ 只清登录相关字段，保留记住密码等信息
        this.token = ''
        this.userId = ''
        this.username = ''
        this.role = 2
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('role')

        ElMessage.success('已退出登录')
      }
    }
  }
})
