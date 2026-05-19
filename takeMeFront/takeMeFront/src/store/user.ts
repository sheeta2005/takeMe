import { defineStore } from 'pinia'
import { getUserInfo as apiGetUserInfo, updateUserInfo as apiUpdateUserInfo } from '@/api/user'

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
    age: null,
    gender: 0, // 0男 1女
    address: '',
    walkingRange: '',
    avatar: ''
  }),

  actions: {
    // 登录存入基础信息
    setUserInfo(token: string, userId: string, username: string, role: number) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = role

      // 持久化到本地存储
      localStorage.setItem('token', token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('role', String(role))
    },

    // 从后端获取完整用户详情（核心方法）
    async getUserInfo() {
      // 👇 后续换成真实接口，直接解开这两行注释即可
      // const res = await apiGetUserInfo()
      // this.$patch(res.data)

      // 🎯 模拟后端返回数据（临时用）
      await new Promise(resolve => setTimeout(resolve, 300)) // 模拟网络延迟
      this.$patch({
        account: 'laoren001',
        phone: '13800138000',
        age: 72,
        gender: 0,
        address: '西安市雁塔区XX小区3号楼2单元101',
        walkingRange: '1000米',
        avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
      })
    },

    // 更新用户信息
    async updateUserInfo(data: any) {
      // 👇 后续换成真实接口
      // await apiUpdateUserInfo(data)

      // 模拟更新成功，同步更新本地store
      this.$patch(data)
      return Promise.resolve()
    },

    // 退出登录清空所有信息
    logout() {
      this.$reset() // 清空所有state字段
      localStorage.clear() // 清空本地存储
    }
  }
})
