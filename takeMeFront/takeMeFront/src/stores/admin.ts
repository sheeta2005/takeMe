import { ref } from 'vue'
import { defineStore } from 'pinia'

// 管理员信息类型
export interface Admin {
  id: number
  username: string
  realName: string
  phone: string
  avatar: string
  createTime?: string
  lastLoginTime?: string
}

export const useAdminStore = defineStore('admin', () => {
  // 管理员信息
  const adminInfo = ref<Admin>({
    id: 1,
    username: 'admin',
    realName: '系统管理员',
    phone: '13800138000',
    avatar: 'https://picsum.photos/200/200', // 默认头像
    createTime: '2025-01-01 00:00:00',
    lastLoginTime: new Date().toLocaleString()
  })

  // 获取管理员信息（模拟接口）
  const getAdminInfo = () => {
    // 这里后期替换成真实接口请求
    console.log('获取管理员信息成功')
  }

  return {
    adminInfo,
    getAdminInfo,

    // 方便页面直接使用
    username: adminInfo.value.username,
    avatar: adminInfo.value.avatar,
    realName: adminInfo.value.realName
  }
})
