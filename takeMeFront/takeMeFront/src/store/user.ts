import { defineStore } from 'pinia'
// import { getUserInfo as apiGetUserInfo, updateUserInfo as apiUpdateUserInfo } from '@/api/user'

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
      // const res = await apiGetUserInfo()
      // this.$patch(res.data)

      // 🎯 模拟后端数据
      await new Promise(resolve => setTimeout(resolve, 300))
      this.$patch({
        account: 'laoren001',
        phone: '13800138000',
        age: 72,
        gender: 0,
        addresses: [
          '西安市雁塔区XX小区3号楼2单元101',
          '西安市雁塔区XX医院住院部',
          '西安市雁塔区XX公园门口'
        ],
        emergencyName: '张建国',
        emergencyPhone: '13900139000',
        avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
      })
    },

    // 更新用户信息
    async updateUserInfo(data: any) {
      // await apiUpdateUserInfo(data)
      this.$patch(data)
      return Promise.resolve()
    },

    // 退出登录清空所有信息
    logout() {
      localStorage.clear()
      this.$reset()
    }
  }
})
