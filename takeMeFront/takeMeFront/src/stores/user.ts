import { defineStore } from 'pinia'
import { getUserInfo, updateUserInfo, logout } from '@/api/user'
import { ElMessage } from 'element-plus'

// ✅ 定义地址对象类型（匹配后端）
interface UserAddress {
  id: number
  address: string
  isDefault: number
  userId?: number
  createTime?: string
}

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
    // ✅ 修复：改为对象数组，不再是string[]
    addresses: [] as UserAddress[],
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

    // ✅ 新增：更新地址列表
    setAddresses(list: UserAddress[]) {
      this.addresses = list
    },

    // 从后端获取完整用户详情
    async getUserInfo() {
      if (!this.token) return false
      try {
        const res = await getUserInfo()
        this.$patch(res.data)
        return true
      } catch (err) {
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
        await logout()
      } catch (err) {} finally {
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
