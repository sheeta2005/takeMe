<template>
  <div class="leave-container">
    <h2 class="page-title">出勤请假</h2>
    <div class="content-wrapper">
      <div class="calendar-card">
        <h3 class="card-title">本月工作日历</h3>
        <div class="calendar-header">
          <el-button size="small" @click="prevMonth">◀</el-button>
          <span class="month-text">{{ currentMonth }}</span>
          <el-button size="small" @click="nextMonth">▶</el-button>
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
        <div class="workday-tip">
          <span class="tip-label">工作日：</span>
          <el-tag v-for="day in workDaysText" :key="day" type="success" size="small" style="margin: 2px;">{{ day }}</el-tag>
        </div>
      </div>

      <div class="right-content">
        <div class="form-card">
          <h3 class="card-title">提交请假申请</h3>
          <el-form :model="leaveForm" label-width="100px">
            <el-form-item label="请假类型">
              <el-select v-model="leaveForm.type" placeholder="请选择">
                <el-option label="事假" :value="0" />
                <el-option label="病假" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="请假时间">
              <el-date-picker
                v-model="leaveForm.time"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD HH:mm:ss"
                :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
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
              <el-button type="primary" @click="submitLeaveForm">提交申请</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="record-card">
          <h3 class="card-title">请假记录</h3>
          <div class="leave-list">
            <el-empty v-if="leaveList.length === 0" description="暂无请假记录" />
            <div class="leave-item" v-for="item in leaveList" :key="item.id">
              <span class="type">{{ item.type === 0 ? '事假' : '病假' }}</span>
              <span class="time">{{ item.startTime }} 至 {{ item.endTime }}</span>
              <el-tag :type="getTagType(item.status)" size="small">
                {{ item.status === 0 ? '待审核' : item.status === 1 ? '已通过' : '已拒绝' }}
              </el-tag>
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
import { useVolunteerStore } from '@/stores/volunteer'
import { submitLeave, getLeaveList } from '@/api/volunteer'

const volunteerStore = useVolunteerStore()

const leaveForm = ref({
  type: undefined as number | undefined,
  time: [] as string[],
  reason: ''
})

const leaveList = ref<any[]>([])

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const currentDate = new Date()
const currentYear = ref(currentDate.getFullYear())
const currentMonthIndex = ref(currentDate.getMonth())

const workDaysArray = computed(() => {
  if (!volunteerStore.serviceDays) return []
  return volunteerStore.serviceDays.split(',').map(d => parseInt(d.trim())).filter(d => !isNaN(d))
})

const workDaysText = computed(() => {
  const dayMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return workDaysArray.value.map(d => dayMap[d])
})

const currentMonth = computed(() => {
  return `${currentYear.value}年${currentMonthIndex.value + 1}月`
})

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

  for (let i = startDay - 1; i >= 0; i--) {
    result.push({
      day: prevMonthDays - i,
      isOtherMonth: true,
      isWorkDay: false,
      isToday: false
    })
  }

  const today = new Date()
  const workDays = workDaysArray.value
  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day)
    const dayOfWeek = date.getDay()
    const isWorkDay = workDays.includes(dayOfWeek)
    const isToday = date.toDateString() === today.toDateString()
    result.push({
      day,
      isOtherMonth: false,
      isWorkDay,
      isToday
    })
  }

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

const getTagType = (status: number) => {
  if (status === 1) return 'success'
  if (status === 0) return 'warning'
  return 'info'
}

const submitLeaveForm = async () => {
  if (!leaveForm.value.type || !leaveForm.value.time.length || !leaveForm.value.reason) {
    ElMessage.warning('请完善请假信息')
    return
  }

  try {
    await submitLeave({
      type: leaveForm.value.type,
      startTime: leaveForm.value.time[0],
      endTime: leaveForm.value.time[1],
      reason: leaveForm.value.reason
    })
    ElMessage.success('请假申请已提交')
    leaveForm.value = { type: undefined, time: [], reason: '' }
    await loadLeaveList()
  } catch {
    ElMessage.error('提交失败，请重试')
  }
}

const loadLeaveList = async () => {
  try {
    const res = await getLeaveList()
    if (res.code === 200) {
      leaveList.value = res.data || []
    }
  } catch {
    ElMessage.error('加载记录失败')
  }
}

onMounted(async () => {
  await volunteerStore.fetchVolunteerInfo()
  loadLeaveList()
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

.calendar-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  width: 320px;
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
  font-size: 14px;
}
.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}
.calendar-day {
  padding: 8px 4px;
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
  font-weight: bold;
}
.calendar-day.today {
  border: 2px solid #006d5c;
  font-weight: bold;
}
.workday-tip {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tip-label {
  font-size: 14px;
  color: #666;
}

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
