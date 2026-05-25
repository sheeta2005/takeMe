import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useVolunteerStore = defineStore('volunteer', () => {
  // 服务相关状态
  const serviceCount = ref(0)
  const serviceHours = ref(0)
  const points = ref(0)
  const isOnLeave = ref(false)

  // 方法
  function updateServiceStats(count: number, hours: number, newPoints: number) {
    serviceCount.value += count
    serviceHours.value += hours
    points.value += newPoints
  }

  function setLeaveStatus(status: boolean) {
    isOnLeave.value = status
  }

  return {
    serviceCount,
    serviceHours,
    points,
    isOnLeave,
    updateServiceStats,
    setLeaveStatus
  }
}, {
  persist: true
})
