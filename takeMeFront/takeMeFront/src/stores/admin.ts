import { defineStore } from 'pinia'
import { getAdminInfo, updateAdminInfo, adminLogout } from '@/api'
import { ElMessage } from 'element-plus'
import wsManager from '@/utils/websocket'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem('adminToken') || '',
    adminId: localStorage.getItem('adminId') || '',
    username: '',
    role: 0,
    realName: '',
    avatar: '',
    createTime: '',
    lastLoginTime: ''
  }),

  actions: {
    setAdminInfo(token: string, adminId: string, username: string, realName: string) {
      this.token = token
      this.adminId = adminId
      this.username = username
      this.realName = realName
      this.role = 0

      localStorage.setItem('adminToken', token)
      localStorage.setItem('adminId', adminId)
    },

    async fetchAdminInfo() {
      if (!this.token) return false
      try {
        const res = await getAdminInfo()
        this.$patch(res.data)
        return true
      } catch (err) {
        return false
      }
    },

    async updateAdminInfo(data: any) {
      try {
        await updateAdminInfo(data)
        this.$patch(data)
        ElMessage.success('信息修改成功')
        return true
      } catch (err) {
        ElMessage.error('修改失败')
        return false
      }
    },

    async logout() {
      try {
        await adminLogout()
      } catch (err) {} finally {
        wsManager.disconnect()
        this.token = ''
        this.adminId = ''
        this.username = ''
        this.realName = ''
        this.avatar = ''
        this.createTime = ''
        this.lastLoginTime = ''
        this.role = 0
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminId')
        ElMessage.success('已退出登录')
      }
    }
  }
})
