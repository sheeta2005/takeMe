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
          :loading="submitting"
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
import { createOrder, getServiceList } from '@/api/order'
import { newOrderRequestId } from '@/utils/orderRequest'
import { getUserAddressList } from '@/api/user'
import type { ServicePackage } from '@/types/ServicePackage'

// import { getServiceTimeSlots } from '@/api/service'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const serviceId = ref<number | null>(null)
const submitting = ref(false)
let requestId = ''
let submittedItems = ''

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
  return allTimeSlots.value.filter(slot => {
    const appointment = new Date(`${selectedDate.value}T${slot.value}:00`)
    return appointment.getTime() > Date.now() + 60 * 60 * 1000
  })
})

const canSubmit = computed(() => {
  if (!serviceId.value || submitting.value) return false
  if (!serviceAddress.value) return false
  if (!selectedDate.value) return false
  if (!selectedTime.value || !showTimeSlots.value.some(slot => slot.value === selectedTime.value)) return false
  if (serviceInfo.value.type === '助医' && !hospital.value) return false
  return true
})

onMounted(async () => {
  try {
    const { serviceType, serviceName } = route.query
    const typeNames: Record<string, number> = { 代购: 0, 助洁: 1, 助餐: 2, 助医: 3, 陪伴: 4 }
    const type = Number.isInteger(Number(serviceType)) && String(serviceType).trim() !== ''
      ? Number(serviceType) : typeNames[String(serviceType)]
    if (!Number.isInteger(type) || type < 0 || type > 4 || !serviceName) {
      ElMessage.error('服务信息错误')
      router.back()
      return
    }
    // 套餐 ID、名称和展示价来自服务端；同名套餐需明确指定 ID。
    const services = (await getServiceList(type)).data as ServicePackage[]
    const matches = services.filter(service => service.name === serviceName
      && (!route.query.serviceId || service.id === Number(route.query.serviceId)))
    if (matches.length !== 1) {
      ElMessage.error('服务已变更，请重新选择')
      router.back()
      return
    }
    const service = matches[0]
    serviceId.value = service.id
    serviceInfo.value = {
      type: Object.keys(typeNames).find(name => typeNames[name] === type) || '',
      name: service.name,
      price: service.price
    }

    // await loadServiceTimeSlots(serviceType as string)

    await userStore.getUserInfo()
    const addressRes = await getUserAddressList()
    const addrs = (addressRes.data || []) as Array<{ address: string; isDefault: number }>
    addressList.value = addrs.map(a => a.address)
    const defaultAddr = addrs.find(a => a.isDefault === 1)?.address || ''
    serviceAddress.value = defaultAddr
  } catch (err) {
    ElMessage.error('页面加载失败')
  }
})

//返回上一页
const goBack = () => router.back()

//提交服务预约订单
const submitOrder = async () => {
  if (!canSubmit.value || !serviceId.value) return
  submitting.value = true
  try {
    const itemRemark = [hospital.value && `就诊医院：${hospital.value}`, remark.value]
      .filter(Boolean).join('；')
    const items = [{
        serviceId: serviceId.value,
        quantity: 1,
        serviceDate: selectedDate.value,
        serviceTime: selectedTime.value,
        address: serviceAddress.value,
        remark: itemRemark
      }]
    // 内容未变化的网络重试复用标识，修改预约内容才开始新提交。
    const fingerprint = JSON.stringify(items)
    if (submittedItems !== fingerprint) {
      requestId = newOrderRequestId()
      submittedItems = fingerprint
    }
    const res = await createOrder({
      order: { requestId },
      items
    })
    const result = res as unknown as { code: number; data?: { id: number }; msg?: string }
    if (result.code !== 200 || !result.data?.id) throw new Error(result.msg || '下单失败')
    ElMessage.success('订单已创建，请完成支付')
    await router.replace(`/user/payment?orderId=${result.data.id}`)
  } catch (err) {
    ElMessage.error('下单失败，请重试')
  } finally {
    submitting.value = false
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
