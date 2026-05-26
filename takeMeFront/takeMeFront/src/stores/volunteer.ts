import { defineStore } from 'pinia'
import { ref } from 'vue'

// 按你给的接口定义
export interface VolunteerInfo {
  // 基础
  realName: string
  username: string
  phone: string
  password?: string
  avatar: string

  // 服务
  serviceDays: string
  serviceType: '代购' | '洁净' | '送餐' | '陪医'
  status: string

  // 扩展
  gender: '男' | '女'
  age: number
  address: string
  emergencyName: string
  emergencyPhone: string

  // 系统（可选）
  volunteerId?: number
  totalServiceHours?: number
  createTime?: string
  lastLoginTime?: string
}

export const useVolunteerStore = defineStore('volunteer', () => {
  // 初始状态：空对象，按接口定义
  const volunteerInfo = ref<VolunteerInfo | null>(null)

  // 积分/服务统计（你之前的业务字段）
  const serviceCount = ref(0)
  const serviceHours = ref(0)
  const points = ref(0)
  const isOnLeave = ref(false)

  // 👉 关键：从 serviceDays 解析出工作日数组（用于日历高亮）
  const workDays = ref<string[]>([])

  // ========== Actions ==========
  // 设置完整志愿者信息
  function setVolunteerInfo(info: VolunteerInfo) {
    volunteerInfo.value = info
    // 同时解析工作日（假设后端返回逗号分隔的字符串，如 "周一,周三,周五"）
    if (info.serviceDays) {
      workDays.value = info.serviceDays.split(',').map(d => d.trim())
    }
  }

  // 清除志愿者信息（退出登录用）
  function clearVolunteerInfo() {
    volunteerInfo.value = null
    workDays.value = []
    serviceCount.value = 0
    serviceHours.value = 0
    points.value = 0
    isOnLeave.value = false
  }

  // 更新服务统计
  function updateServiceStats(count: number, hours: number, newPoints: number) {
    serviceCount.value += count
    serviceHours.value += hours
    points.value += newPoints
  }

  // 设置请假状态
  function setLeaveStatus(status: boolean) {
    isOnLeave.value = status
  }

  return {
    // 状态
    volunteerInfo,
    serviceCount,
    serviceHours,
    points,
    isOnLeave,
    workDays,

    // 方法
    setVolunteerInfo,
    clearVolunteerInfo,
    updateServiceStats,
    setLeaveStatus
  }
}, {
  persist: true // 持久化，刷新页面不丢状态
})
