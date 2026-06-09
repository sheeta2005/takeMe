import { defineStore } from 'pinia'
import { getVolunteerInfo, updateVolunteerInfo, volunteerLogout, uploadAvatar } from '@/api/volunteer'
import { ElMessage } from 'element-plus'

export interface VolunteerInfo {
  id: number
  status: number
  createTime: string
  lastLoginTime: string

  realName: string
  username: string
  phone: string
  password?: string
  avatar: string

  gender: number
  age: number | null
  address: string

  serviceDays: string

  workStatus: number
  totalServiceHours: number

  emergencyName: string
  emergencyPhone: string
}

export const useVolunteerStore = defineStore('volunteer', {
  state: () => ({
    token: localStorage.getItem('volunteerToken') || '',
    userId: localStorage.getItem('volunteerId') || '',
    username: localStorage.getItem('volunteerUsername') || '',
    role: Number(localStorage.getItem('volunteerRole')) || 1,

    realName: '',
    phone: '',
    avatar: '',
    gender: 0,
    age: null as number | null,
    address: '',
    serviceDays: '',

    workStatus: 0,
    totalServiceHours: 0,
    emergencyName: '',
    emergencyPhone: '',
    status: 0,
    createTime: '',
    lastLoginTime: ''
  }),

  getters: {
    workDaysArray: (state) => {
      if (!state.serviceDays) return []
      return state.serviceDays.split(',').map(d => parseInt(d.trim()))
    },

    serviceDayText: (state) => {
      const dayMap = ['周天', '周一', '周二', '周三', '周四', '周五', '周六']
      if (!state.serviceDays) return '未设置'
      return state.serviceDays.split(',').map(d => dayMap[parseInt(d.trim())]).join('、')
    },


    workStatusText: (state) => {
      const statusMap = ['休息中', '待命中', '服务中']
      return statusMap[state.workStatus] || '未知'
    }
  },

  actions: {
    setVolunteerInfo(token: string, userId: string, username: string) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = 1

      localStorage.setItem('volunteerToken', token)
      localStorage.setItem('volunteerId', userId)
      localStorage.setItem('volunteerUsername', username)
      localStorage.setItem('volunteerRole', '1')
    },

    async fetchVolunteerInfo() {
      if (!this.token) return false
      try {
        const res = await getVolunteerInfo()
        this.$patch(res.data)
        return true
      } catch (err) {
        ElMessage.error('获取志愿者信息失败')
        return false
      }
    },

    async updateVolunteerInfo(data: Partial<VolunteerInfo>) {
      try {
        await updateVolunteerInfo(data)
        this.$patch(data)
        ElMessage.success('信息修改成功')
        return true
      } catch (err) {
        ElMessage.error('修改失败')
        return false
      }
    },

    async uploadAvatar(file: File) {
      try {
        const res = await uploadAvatar(file)
        if (res.code === 200 && res.data?.url) {
          this.avatar = res.data.url
          ElMessage.success('头像上传成功')
          return res.data.url
        }
        return null
      } catch (err) {
        ElMessage.error('头像上传失败')
        return null
      }
    },

    addServiceHours(hours: number) {
      this.totalServiceHours += hours
    },

    setWorkStatus(status: 0 | 1 | 2) {
      this.workStatus = status
    },

    async logout() {
      try {
        await volunteerLogout()
      } catch (err) {
      } finally {
        this.$reset()
        localStorage.removeItem('volunteerToken')
        localStorage.removeItem('volunteerId')
        localStorage.removeItem('volunteerUsername')
        localStorage.removeItem('volunteerRole')
        ElMessage.success('已退出登录')
      }
    }
  }
})
