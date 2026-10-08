import { defineStore } from 'pinia'
import { getUserInfo, updateUserInfo, logout } from '@/api/user'
import { ElMessage } from 'element-plus'
import wsManager from '@/utils/websocket'

interface UserAddress {
  id: number
  address: string
  isDefault: number
  userId?: number
  createTime?: string
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: '',
    role: Number(localStorage.getItem('role')) || 2,
    realName: '',
    phone: '',
    age: null as number | null,
    gender: 0,
    addresses: [] as UserAddress[],
    emergencyName: '',
    emergencyPhone: '',
    avatar: ''
  }),

  actions: {
    //保存用户登录信息
    setUserInfo(token: string, userId: string, username: string, role: number) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = role

      localStorage.setItem('token', token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('role', String(role))
    },

    //保存用户地址列表
    setAddresses(list: UserAddress[]) {
      this.addresses = list
    },

    //加载并同步用户资料
    async getUserInfo() {
      if (!this.token) return false
      try {
        const res = await getUserInfo()
        const userData = res.data
        this.realName = userData.realName || ''
        this.username = userData.username || ''
        this.phone = userData.phone || ''
        this.age = userData.age || null
        this.gender = userData.gender ?? 0
        this.emergencyName = userData.emergencyName || ''
        this.emergencyPhone = userData.emergencyPhone || ''
        this.avatar = userData.avatar || ''
        return true
      } catch (err) {
        console.error('获取用户信息失败:', err)
        return false
      }
    },

    //修改并同步用户资料
    async updateUserInfo(data: any) {
      try {
        await updateUserInfo(data)
        if (data.realName !== undefined) this.realName = data.realName
        if (data.phone !== undefined) this.phone = data.phone
        if (data.age !== undefined) this.age = data.age
        if (data.gender !== undefined) this.gender = data.gender
        if (data.emergencyName !== undefined) this.emergencyName = data.emergencyName
        if (data.emergencyPhone !== undefined) this.emergencyPhone = data.emergencyPhone
        if (data.avatar !== undefined) this.avatar = data.avatar
        ElMessage.success('信息修改成功')
        return true
      } catch (err) {
        console.error('修改用户信息失败:', err)
        ElMessage.error('修改失败')
        return false
      }
    },

    //退出并清理用户登录状态
    async logout() {
      try {
        await logout()
      } catch (err) {} finally {
        wsManager.disconnect()
        this.token = ''
        this.userId = ''
        this.username = ''
        this.role = 2
        this.realName = ''
        this.phone = ''
        this.age = null
        this.gender = 0
        this.emergencyName = ''
        this.emergencyPhone = ''
        this.avatar = ''
        this.addresses = []
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('role')
        ElMessage.success('已退出登录')
      }
    }
  }
})
