<template>
  <div class="order-page">
    <h2 class="page-title">确认下单</h2>

    <div class="order-card">
      <!-- 服务信息 -->
      <div class="service-info">
        <div class="service-name">{{ serviceInfo.name }}</div>
        <div class="service-desc">{{ serviceInfo.desc }}</div>
        <div class="service-price">¥{{ serviceInfo.price }}</div>
      </div>

      <!-- ====================== -->
      <!-- 🔥 新增：服务地址（所有服务都显示） -->
      <!-- ====================== -->
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

      <!-- 助餐：要求送达时间（最小半小时后） -->
      <div class="form-section" v-if="serviceInfo.type === '助餐'">
        <div class="label">要求送达时间 *</div>
        <el-date-picker
          v-model="deliveryTime"
          type="datetime"
          placeholder="请选择送达时间"
          size="large"
          style="width: 100%;"
          :disabled-date="disabledDate"
          :disabled-time="disabledDeliveryTime"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
        ></el-date-picker>
      </div>

      <!-- 助洁：上门时间 -->
      <div class="form-section" v-if="serviceInfo.type === '助洁'">
        <div class="label">上门时间 *</div>
        <el-date-picker
          v-model="serviceTime"
          type="datetime"
          placeholder="请选择上门时间"
          size="large"
          style="width: 100%;"
          :disabled-date="disabledDate"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
        ></el-date-picker>
      </div>

      <!-- 助医：就诊医院 + 就诊时间 -->
      <div class="form-section" v-if="serviceInfo.type === '助医'">
        <div class="label">就诊医院 *</div>
        <el-input
          v-model="hospital"
          placeholder="请输入就诊医院名称"
          size="large"
        ></el-input>

        <div class="label" style="margin-top: 20px;">就诊时间 *</div>
        <el-date-picker
          v-model="serviceTime"
          type="datetime"
          placeholder="请选择就诊时间"
          size="large"
          style="width: 100%;"
          :disabled-date="disabledDate"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
        ></el-date-picker>
      </div>

      <!-- 代购：期望送达时间（已删除购买地址） -->
      <div class="form-section" v-if="serviceInfo.type === '代购'">
        <div class="label">期望送达时间 *</div>
        <el-date-picker
          v-model="deliveryTime"
          type="datetime"
          placeholder="请选择期望送达时间"
          size="large"
          style="width: 100%;"
          :disabled-date="disabledDate"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
        ></el-date-picker>
      </div>

      <!-- 通用备注 -->
      <div class="form-section">
        <div class="label">备注信息（选填）</div>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="3"
          placeholder="请输入您的特殊要求，比如：不要香菜、上午上门"
          size="large"
        ></el-input>
      </div>

      <!-- 按钮组：取消 + 确认下单 -->
      <div class="submit-section">
        <el-button type="default" size="large" class="cancel-btn" @click="$router.back()">
          取消
        </el-button>
        <el-button type="primary" size="large" class="submit-btn" @click="submitOrder">
          确认下单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

// 🔥 引入用户store
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 服务基础信息
const serviceInfo = ref({
  type: '',
  name: '',
  desc: '',
  price: 0
})

// 通用字段
const remark = ref('')

// 助餐/代购 字段
const deliveryTime = ref('')

// 助洁/助医 字段
const serviceTime = ref('')

// 助医 字段
const hospital = ref('')

// ======================
// 🔥 新增：地址相关
// ======================
const serviceAddress = ref('') // 当前选中地址
const addressList = ref<string[]>([]) // 地址下拉列表

// 禁用今天之前的日期
const disabledDate = (time: Date) => {
  return time.getTime() < Date.now() - 8.64e7
}

// 助餐：禁用当前时间30分钟之前的时间
const disabledDeliveryTime = (time: Date) => {
  const now = new Date()
  const minTime = new Date(now.getTime() + 30 * 60 * 1000) // 30分钟后

  if (time.toDateString() === now.toDateString()) {
    return time.getTime() < minTime.getTime()
  }
  return false
}

// 加载服务信息
const loadServiceInfo = () => {
  const { type, name, desc, price } = route.query
  if (type && name && desc && price) {
    serviceInfo.value = {
      type: type as string,
      name: name as string,
      desc: desc as string,
      price: Number(price)
    }
  } else {
    ElMessage.error('服务信息错误')
    router.back()
  }
}

// ======================
// 🔥 新增：加载用户地址
// ======================
const loadUserAddress = async () => {
  // 从store获取用户信息
  await userStore.getUserInfo()

  // 默认地址 = store里的用户地址
  const defaultAddr = userStore.address || '暂未设置地址'

  // 设置下拉列表（可自行扩展更多）
  addressList.value = [
    defaultAddr,
    '西安市雁塔区科技二路 66 号',
    '西安市高新区绿地中心 A 座 1202'
  ]

  // 默认选中默认地址
  serviceAddress.value = defaultAddr
}

// 提交订单
const submitOrder = async () => {
  // ======================
  // 🔥 校验地址
  // ======================
  if (!serviceAddress.value) {
    return ElMessage.error('请选择服务地址')
  }

  // 基础校验
  if (serviceInfo.type === '助餐' && !deliveryTime.value) {
    return ElMessage.error('请选择送达时间')
  }
  if (serviceInfo.type === '助洁' && !serviceTime.value) {
    return ElMessage.error('请选择上门时间')
  }
  if (serviceInfo.type === '助医') {
    if (!hospital.value) return ElMessage.error('请输入就诊医院')
    if (!serviceTime.value) return ElMessage.error('请选择就诊时间')
  }
  if (serviceInfo.type === '代购' && !deliveryTime.value) {
    return ElMessage.error('请选择期望送达时间')
  }

  // 组装订单数据
  const orderData = {
    serviceType: serviceInfo.value.type,
    serviceName: serviceInfo.value.name,
    price: serviceInfo.value.price,
    remark: remark.value,
    serviceAddress: serviceAddress.value, // 🔥 提交时带上地址
    ...(serviceInfo.type === '助餐' && { deliveryTime: deliveryTime.value }),
    ...(serviceInfo.type === '助洁' && { serviceTime: serviceTime.value }),
    ...(serviceInfo.type === '助医' && {
      hospital: hospital.value,
      serviceTime: serviceTime.value
    }),
    ...(serviceInfo.type === '代购' && {
      deliveryTime: deliveryTime.value
    })
  }

  // 模拟提交
  ElMessage.success('下单成功！\n服务地址：' + serviceAddress.value)
  router.push('/user/order')
}

onMounted(() => {
  loadServiceInfo()
  loadUserAddress() // 🔥 加载地址
})
</script>

<style scoped>
.order-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 30px 0;
  text-align: center;
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
  font-size: 22px;
  font-weight: 600;
  color: #006d5c;
  margin-bottom: 8px;
}

.service-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 12px;
}

.service-price {
  font-size: 24px;
  color: #f56c6c;
  font-weight: bold;
}

.form-section {
  margin-bottom: 30px;
}

.label {
  font-size: 18px;
  color: #333;
  margin-bottom: 12px;
}

.submit-section {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.cancel-btn, .submit-btn {
  font-size: 20px;
  padding: 14px 60px;
}
</style>
