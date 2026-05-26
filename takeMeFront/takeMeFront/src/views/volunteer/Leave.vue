<template>
  <div class="leave-container">
    <h2 class="page-title">出勤请假</h2>
    <div class="content-wrapper">
      <!-- 左侧：本月日历 -->
      <div class="calendar-card">
        <h3 class="card-title">本月工作日历</h3>
        <div class="calendar-header">
          <el-button size="small" icon="el-icon-arrow-left" @click="prevMonth"></el-button>
          <span class="month-text">{{ currentMonth }}</span>
          <el-button size="small" icon="el-icon-arrow-right" @click="nextMonth"></el-button>
        </div>
        <div class="calendar-weekdays">
          <span v-for="day in weekdays" :key="day">{{ day }}</span>
        </div>
        <div class="calendar-days">
          <div
            v-for="date in calendarDays"
            :key="date.day"
            class="calendar-day"
            :class="{
              other: date.isOtherMonth,
              workday: date.isWorkDay,
              today: date.isToday
            }"
          >
            {{ date.day }}
          </div>
        </div>
      </div>

      <!-- 右侧：表单 + 记录 -->
      <div class="right-content">
        <div class="form-card">
          <h3 class="card-title">提交请假申请</h3>
          <el-form :model="leaveForm" label-width="100px">
            <el-form-item label="请假类型">
              <el-select v-model="leaveForm.type" placeholder="请选择">
                <el-option label="事假" value="事假" />
                <el-option label="病假" value="病假" />
              </el-select>
            </el-form-item>
            <el-form-item label="请假时间">
              <el-date-picker
                v-model="leaveForm.time"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
            <el-form-item label="请假原因">
              <el-input
                v-model="leaveForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入请假原因"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitLeave">提交申请</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="record-card">
          <h3 class="card-title">请假记录</h3>
          <div class="leave-list">
            <div class="leave-item" v-for="item in leaveList" :key="item.id">
              <span class="type">{{ item.type }}</span>
              <span class="time">{{ item.time }}</span>
              <el-tag :type="getTagType(item.status)" size="small">{{ item.status }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

// 👉 替换成你的 Pinia
import { useVolunteerStore } from '@/stores/volunteer'

// =========== 后端接口（先注释） ===========
// import { submitLeaveApi, getLeaveListApi } from '@/api/volunteer'

// 👉 使用 Pinia
const volunteerStore = useVolunteerStore()

// 请假表单
const leaveForm = ref({
  type: '' as '事假' | '病假',
  time: [] as string[],
  reason: ''
})

// 请假记录类型
interface LeaveRecord {
  id: number
  type: '事假' | '病假'
  time: string
  status: '已通过' | '待审核' | '已拒绝'
}

// 请假记录
const leaveList = ref<LeaveRecord[]>([
  { id: 1, type: '事假', time: '2026-05-25 至 2026-05-26', status: '已通过' }
])

// 日历相关
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const currentDate = new Date()
const currentYear = ref(currentDate.getFullYear())
const currentMonthIndex = ref(currentDate.getMonth())

// 👉 从 PINIA 获取工作周几（自动解析 serviceDays）
const userWorkDays = computed(() => {
  return volunteerStore.workDays || []
})

// 月份文本
const currentMonth = computed(() => {
  return `${currentYear.value}年${currentMonthIndex.value + 1}月`
})

// 生成日历日期（含是否工作日）
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonthIndex.value
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDay = firstDay.getDay()
  const daysInMonth = lastDay.getDate()

  const result: any[] = []
  const prevMonth = new Date(year, month, 0)
  const prevMonthDays = prevMonth.getDate()

  // 上个月末尾补全
  for (let i = startDay - 1; i >= 0; i--) {
    result.push({
      day: prevMonthDays - i,
      isOtherMonth: true,
      isWorkDay: false,
      isToday: false
    })
  }

  // 当前月日期
  const today = new Date()
  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day)
    const dayOfWeek = weekdays[date.getDay()]
    const isWorkDay = userWorkDays.value.includes(dayOfWeek)
    const isToday = date.toDateString() === today.toDateString()
    result.push({
      day,
      isOtherMonth: false,
      isWorkDay,
      isToday
    })
  }

  // 下个月开头补全
  const totalCells = 42
  const nextMonthStart = 1
  for (let i = result.length; i < totalCells; i++) {
    result.push({
      day: nextMonthStart + (i - result.length),
      isOtherMonth: true,
      isWorkDay: false,
      isToday: false
    })
  }

  return result
})

// 上/下一月
const prevMonth = () => {
  if (currentMonthIndex.value === 0) {
    currentMonthIndex.value = 11
    currentYear.value--
  } else {
    currentMonthIndex.value--
  }
}
const nextMonth = () => {
  if (currentMonthIndex.value === 11) {
    currentMonthIndex.value = 0
    currentYear.value++
  } else {
    currentMonthIndex.value++
  }
}

// 请假记录状态标签
const getTagType = (status: string) => {
  if (status === '已通过') return 'success'
  if (status === '待审核') return 'warning'
  return 'info'
}

// 提交请假申请
const submitLeave = async () => {
  if (!leaveForm.value.type || !leaveForm.value.time.length) {
    ElMessage.warning('请完善请假信息')
    return
  }

  // =========== 后端联调时打开 ===========
  // try {
  //   await submitLeaveApi({
  //     type: leaveForm.value.type,
  //     startDate: leaveForm.value.time[0],
  //     endDate: leaveForm.value.time[1],
  //     reason: leaveForm.value.reason
  //   })
  //   ElMessage.success('请假申请已提交')
  //   fetchLeaveList()
  // } catch {
  //   ElMessage.error('提交失败，请重试')
  // }

  // 模拟提交
  ElMessage.success('请假申请已提交（模拟）')
  leaveForm.value = { type: '' as '事假' | '病假', time: [], reason: '' }
}

// 获取请假记录（后端接口）
const fetchLeaveList = async () => {
  // =========== 后端联调时打开 ===========
  // try {
  //   const res = await getLeaveListApi()
  //   leaveList.value = res.data
  // } catch {
  //   ElMessage.error('加载记录失败')
  // }
}

onMounted(() => {
  fetchLeaveList()
})
</script>

<style scoped>
.leave-container {
  max-width: 1200px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.content-wrapper {
  display: flex;
  gap: 24px;
}

/* 左侧日历卡片 */
.calendar-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  width: 300px;
}
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.month-text {
  font-size: 18px;
  font-weight: bold;
}
.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 8px;
  text-align: center;
  color: #666;
}
.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}
.calendar-day {
  padding: 8px;
  text-align: center;
  border-radius: 4px;
  font-size: 14px;
  cursor: default;
}
.calendar-day.other {
  color: #ccc;
}
.calendar-day.workday {
  background-color: #00b899;
  color: #fff;
}
.calendar-day.today {
  border: 1px solid #00b899;
  font-weight: bold;
}

/* 右侧内容 */
.right-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.form-card,
.record-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.card-title {
  font-size: 22px;
  color: #333;
  margin-bottom: 16px;
}
.leave-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.leave-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.leave-item:last-child {
  border-bottom: none;
}
.type {
  font-size: 16px;
  color: #333;
}
.time {
  font-size: 16px;
  color: #666;
}
</style>
