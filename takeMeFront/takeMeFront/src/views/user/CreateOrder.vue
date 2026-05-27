<template>
  <div class="order-page">
    <h2 class="page-title">确认下单</h2>

    <div class="order-card">
      <div class="service-info">
        <div class="service-name">{{ serviceInfo.name || '加载中...' }}</div>
        <div class="service-price">¥{{ serviceInfo.price || 0 }}</div>
      </div>

      <div class="form-section">
        <div class="label">服务地址 *</div>
        <el-select
          v-model="serviceAddress"
          placeholder="请选择服务地址"
          size="large"
          style="width: 100%"
        >
          <el-option
            v-for="(addr, index) in addressList"
            :key="index"
            :label="addr"
            :value="addr"
          />
        </el-select>
      </div>

      <div class="form-section">
        <div class="label">选择日期 *</div>
        <div class="date-buttons">
          <el-button
            v-for="day in dateList"
            :key="day.value"
            :type="selectedDate === day.value ? 'primary' : 'default'"
            size="large"
            @click="selectedDate = day.value"
          >
            {{ day.label }}
          </el-button>
        </div>
      </div>

      <div class="form-section" v-if="selectedDate">
        <div class="label">选择时间 *</div>
        <div class="time-buttons">
          <el-button
            v-for="slot in showTimeSlots"
            :key="slot.value"
            :type="selectedTime === slot.value ? 'primary' : 'default'"
            size="large"
            @click="selectedTime = slot.value"
          >
            {{ slot.label }}
          </el-button>
        </div>
        <div class="tip-text" v-if="showTimeSlots.length === 0">
          ⚠️ 今天可预约时间已结束，请选择明天或后天
        </div>
      </div>

      <div class="form-section" v-if="serviceInfo.type === '助医'">
        <div class="label">就诊医院 *</div>
        <el-input
          v-model="hospital"
          placeholder="请输入就诊医院名称"
          size="large"
        ></el-input>
      </div>

      <div class="form-section">
        <div class="label">备注（选填）</div>
        <el-input
          v-model="remark"
          type="textarea"
          rows="2"
          placeholder="有特殊要求请写在这里"
          size="large"
        ></el-input>
      </div>

      <div class="submit-section">
        <el-button type="default" size="large" @click="goBack">取消</el-button>
        <el-button
          type="primary"
          size="large"
          @click="submitOrder"
          :disabled="!canSubmit"
        >
          确认下单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// import { createOrder } from '@/api/order'
// import { getServiceTimeSlots } from '@/api/service'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const serviceInfo = ref({
  type: '',
  name: '',
  price: 0
})

const remark = ref('')
const selectedDate = ref('')
const selectedTime = ref('')
const hospital = ref('')
const serviceAddress = ref('')
const addressList = ref<string[]>([])

const now = new Date()
const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

const dateList = computed(() => {
  const days = []
  for (let i = 0; i < 3; i++) {
    const d = new Date(today.getTime() + i * 24 * 60 * 60 * 1000)
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    const weekDay = weekDays[d.getDay()]
    const label = i === 0 ? `今天 ${weekDay}` : i === 1 ? `明天 ${weekDay}` : `后天 ${weekDay}`
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ label, value })
  }
  return days
})

const allTimeSlots = ref([
  { value: '09:00', label: '上午 9点' },
  { value: '10:00', label: '上午 10点' },
  { value: '11:00', label: '上午 11点' },
  { value: '14:00', label: '下午 2点' },
  { value: '15:00', label: '下午 3点' },
  { value: '16:00', label: '下午 4点' },
  { value: '17:00', label: '下午 5点' }
])

/*
const loadServiceTimeSlots = async (serviceType: string) => {
  try {
    const res = await getServiceTimeSlots(serviceType)
    allTimeSlots.value = res.data || []
  } catch (e) {
    allTimeSlots.value = [
      { value: '09:00', label: '上午 9点' },
      { value: '10:00', label: '上午 10点' },
      { value: '11:00', label: '上午 11点' },
      { value: '14:00', label: '下午 2点' },
      { value: '15:00', label: '下午 3点' },
      { value: '16:00', label: '下午 4点' },
      { value: '17:00', label: '下午 5点' }
    ]
  }
}
*/

const showTimeSlots = computed(() => {
  if (selectedDate.value !== dateList.value[0].value) return allTimeSlots.value
  const currentHour = now.getHours()
  return allTimeSlots.value.filter(slot => {
    const h = parseInt(slot.value.split(':')[0])
    return h > currentHour
  })
})

const canSubmit = computed(() => {
  if (!serviceAddress.value) return false
  if (!selectedDate.value) return false
  if (!selectedTime.value) return false
  if (serviceInfo.value.type === '助医' && !hospital.value) return false
  return true
})

onMounted(async () => {
  try {
    const { serviceType, serviceName, price } = route.query
    if (!serviceType || !serviceName || !price) {
      ElMessage.error('服务信息错误')
      router.back()
      return
    }
    serviceInfo.value = {
      type: serviceType as string,
      name: serviceName as string,
      price: Number(price)
    }

    // await loadServiceTimeSlots(serviceType as string)

    await userStore.getUserInfo()
    const addrs = userStore.addresses || []
    addressList.value = addrs.map(a => a.address)
    const defaultAddr = addrs.find(a => a.isDefault)?.address || ''
    serviceAddress.value = defaultAddr
  } catch (err) {
    ElMessage.error('页面加载失败')
  }
})

const goBack = () => router.back()

const submitOrder = async () => {
  try {
    const finalTime = `${selectedDate.value} ${selectedTime.value}:00`
    ElMessage.success('下单成功！正在为您安排志愿者')
    setTimeout(() => router.push('/user/order'), 800)

    /*
    await createOrder({
      serviceName: serviceInfo.value.name,
      price: serviceInfo.value.price,
      address: serviceAddress.value,
      serviceTime: finalTime,
      remark: remark.value,
      type: serviceInfo.value.type,
      hospital: hospital.value
    })
    ElMessage.success('下单成功！')
    setTimeout(() => router.push('/user/order'), 800)
    */
  } catch (err) {
    ElMessage.error('下单失败，请重试')
  }
}
</script>

<style scoped>
.order-page {
  max-width: 700px;
  margin: 40px auto;
  padding: 0 20px;
}
.page-title {
  font-size: 32px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.order-card {
  background: #fff;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.service-info {
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 30px;
}
.service-name {
  font-size: 26px;
  font-weight: 600;
  color: #06c;
}
.service-price {
  font-size: 28px;
  color: #f5222d;
  font-weight: bold;
  margin-top: 12px;
}
.form-section {
  margin-bottom: 30px;
}
.label {
  font-size: 22px;
  color: #333;
  margin-bottom: 16px;
}
.date-buttons, .time-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.date-buttons .el-button, .time-buttons .el-button {
  height: 56px;
  font-size: 20px !important;
}
.submit-section {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
}
.cancel-btn, .submit-btn {
  font-size: 22px;
  padding: 16px 60px;
}
.tip-text {
  font-size: 18px;
  color: #fa8c16;
  margin-top: 12px;
  text-align: center;
}
:deep(.el-select__wrapper), :deep(.el-textarea__inner) {
  font-size: 20px !important;
  min-height: 50px !important;
}
</style>
