import { defineStore } from 'pinia'
import { getVolunteerInfo, updateVolunteerInfo, volunteerLogout } from '@/api/volunteer'
import { ElMessage } from 'element-plus'

// 志愿者信息接口（和后端Volunteer.java完全一一对应）
export interface VolunteerInfo {
  // 主键与系统字段
  id: number
  status: number // 0待审核 1已认证 2已禁用
  createTime: string
  lastLoginTime: string

  // 基础信息
  realName: string
  username: string
  phone: string
  password?: string
  avatar: string

  // 个人信息
  gender: number // 0男 1女
  age: number | null
  address: string

  // 服务信息（重点修正：和后端一致为Integer类型）
  serviceDays: number // 0=周天 1=周一 2=周二 3=周三 4=周四 5=周五 6=周六
  serviceType: number // 0=代购 1=助洁 2=助餐 3=助医 4=陪伴
  workStatus: number // 0空闲 1服务中 2请假
  totalServiceHours: number

  // 紧急联系人
  emergencyName: string
  emergencyPhone: string
}

export const useVolunteerStore = defineStore('volunteer', {
  state: () => ({
    // 基础登录信息（和user.ts完全对齐，独立持久化避免身份冲突）
    token: localStorage.getItem('volunteerToken') || '',
    userId: localStorage.getItem('volunteerId') || '',
    username: localStorage.getItem('volunteerUsername') || '',
    role: Number(localStorage.getItem('volunteerRole')) || 1, // 志愿者固定身份=1

    // 完整志愿者详情（和后端字段一一对应）
    realName: '',
    phone: '',
    avatar: '',
    gender: 0,
    age: null as number | null,
    address: '',
    serviceDays: 0,
    serviceType: 0,
    workStatus: 0,
    totalServiceHours: 0,
    emergencyName: '',
    emergencyPhone: '',
    status: 0,
    createTime: '',
    lastLoginTime: ''
  }),

  getters: {
    // ✅ 自动转换服务日为中文（前端直接用，不用每次转换）
    serviceDayText: (state) => {
      const dayMap = ['周天', '周一', '周二', '周三', '周四', '周五', '周六']
      return dayMap[state.serviceDays] || '未设置'
    },

    // ✅ 自动转换服务类型为中文
    serviceTypeText: (state) => {
      const typeMap = ['代购服务', '助洁服务', '助餐服务', '助医服务', '陪伴服务']
      return typeMap[state.serviceType] || '未设置'
    },

    // ✅ 自动转换工作状态为中文
    workStatusText: (state) => {
      const statusMap = ['空闲', '服务中', '请假中']
      return statusMap[state.workStatus] || '未知'
    }
  },

  actions: {
    /**
     * 登录存入基础信息（和user.ts的setUserInfo完全对齐）
     */
    setVolunteerInfo(token: string, userId: string, username: string) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = 1

      // 持久化到localStorage
      localStorage.setItem('volunteerToken', token)
      localStorage.setItem('volunteerId', userId)
      localStorage.setItem('volunteerUsername', username)
      localStorage.setItem('volunteerRole', '1')
    },

    /**
     * 从后端获取完整志愿者详情
     */
    async getVolunteerInfo() {
      if (!this.token) return false
      try {
        const res = await getVolunteerInfo()
        // 批量更新所有字段，自动匹配后端返回
        this.$patch(res.data)
        return true
      } catch (err) {
        ElMessage.error('获取志愿者信息失败')
        return false
      }
    },

    /**
     * 更新志愿者个人信息
     */
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

    /**
     * 更新服务时长
     */
    addServiceHours(hours: number) {
      this.totalServiceHours += hours
    },

    /**
     * 更新工作状态
     */
    setWorkStatus(status: 0 | 1 | 2) {
      this.workStatus = status
    },

    /**
     * 退出登录（和user.ts的logout完全对齐）
     */
    async logout() {
      try {
        await volunteerLogout()
      } catch (err) {
        // 忽略退出接口错误，强制清除本地状态
      } finally {
        // 重置所有状态
        this.$reset()
        // 清除localStorage
        localStorage.removeItem('volunteerToken')
        localStorage.removeItem('volunteerId')
        localStorage.removeItem('volunteerUsername')
        localStorage.removeItem('volunteerRole')
        ElMessage.success('已退出登录')
      }
    }
  }
})
