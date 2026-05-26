<template>
  <div class="order-page">
    <h2 class="page-title">确认下单</h2>

    <div class="order-card">
      <!-- 服务信息 -->
      <div class="service-info">
        <div class="service-name">{{ serviceInfo.name || '加载中...' }}</div>
        <div class="service-desc">{{ serviceInfo.desc || '' }}</div>
        <div class="service-price">¥{{ serviceInfo.price || 0 }}</div>
      </div>

      <!-- 服务地址 -->
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

      <!-- 时间选择器（加了自定义类名，不会污染全局） -->
      <div class="form-section" v-if="serviceInfo.type">
        <div class="label">
          {{ serviceInfo.type === '助餐' || serviceInfo.type === '代购' ? '送达时间 *' : '上门时间 *' }}
        </div>
        <div class="picker-wrap">
          <el-date-picker
            v-model="selectedTime"
            type="datetime"
            placeholder="请选择时间"
            size="large"
            style="width: 100%;"
            :disabled-date="disabledDate"
            :disabled-time="disabledTime"
            format="YYYY年MM月DD日 HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :default-value="defaultTime"
            popper-class="custom-date-picker"
          ></el-date-picker>
          <div class="tip-text">⚠️ 请选择当前时间30分钟后至2天内的时间</div>
        </div>
      </div>

      <!-- 助医专属 -->
      <div class="form-section" v-if="serviceInfo.type === '助医'">
        <div class="label">就诊医院 *</div>
        <el-input
          v-model="hospital"
          placeholder="请输入就诊医院名称"
          size="large"
        ></el-input>
      </div>

      <!-- 备注 -->
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

      <!-- 按钮组 -->
      <div class="submit-section">
        <el-button type="default" size="large" class="cancel-btn" @click="goBack">
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
import { useUserStore } from '@/stores/user'
import { createOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 服务信息
const serviceInfo = ref({
  type: '',
  name: '',
  desc: '',
  price: 0
})

// 表单数据
const remark = ref('')
const selectedTime = ref('')
const hospital = ref('')
const serviceAddress = ref('')
const addressList = ref<string[]>([])

// ======================
// 时间逻辑（彻底修复跳转bug）
// ======================
const now = new Date()
const minTime = new Date(now.getTime() + 30 * 60 * 1000)
const maxTime = new Date(now.getTime() + 48 * 60 * 60 * 1000)
const defaultTime = minTime

// 日期禁用：今天、明天、后天 三天可选
const disabledDate = (time: Date) => {
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const twoDaysLater = new Date(today.getTime() + 2 * 24 * 60 * 60 * 1000)
  const compare = new Date(time.getFullYear(), time.getMonth(), time.getDate())
  return compare < today || compare > twoDaysLater
}

// 时间禁用：只有今天限制30分钟后
const disabledTime = (date: Date) => {
  const isToday =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()

  if (!isToday) return { disabledHours: () => [], disabledMinutes: () => [] }

  const minHour = minTime.getHours()
  const minMinute = minTime.getMinutes()
  return {
    disabledHours: () => Array.from({ length: minHour }, (_, i) => i),
    disabledMinutes: (hour: number) => hour === minHour ? Array.from({ length: minMinute }, (_, i) => i) : []
  }
}

// ======================
// 页面加载（修复userStore错误导致的跳转失效）
// ======================
onMounted(async () => {
  try {
    // 1. 接收参数
    const { type, name, desc, price } = route.query
    if (!type || !name || !desc || !price) {
      ElMessage.error('服务信息不完整')
      router.back()
      return
    }
    serviceInfo.value = { type, name, desc, price: Number(price) }

    // 2. 加载地址（加错误处理，即使失败也不影响页面）
    try {
      await userStore.getUserInfo()
      const defaultAddr = userStore.address || '暂未设置地址'
      addressList.value = [defaultAddr, '西安市雁塔区科技二路 66 号', '西安市高新区绿地中心 A 座 1202']
      serviceAddress.value = defaultAddr
    } catch (e) {
      console.error('地址加载失败，使用默认值')
      addressList.value = ['暂未设置地址']
      serviceAddress.value = '暂未设置地址'
    }

  } catch (err) {
    console.error('页面加载失败：', err)
    ElMessage.error('页面加载失败，请刷新重试')
  }
})

// 返回上一页
const goBack = () => {
  router.back()
}

// 提交订单（用router.push代替window.location.href，避免刷新页面）
// 提交订单（先用模拟成功，真实API代码已写好并注释）
const submitOrder = async () => {
  try {
    // 1. 表单校验
    if (!serviceAddress.value) {
      ElMessage.warning('请选择服务地址')
      return
    }
    if (!selectedTime.value) {
      ElMessage.warning('请选择时间')
      return
    }
    if (serviceInfo.value.type === '助医' && !hospital.value) {
      ElMessage.warning('请输入就诊医院')
      return
    }

    // ==============================================
    // 🔥 目前使用：模拟下单成功（不请求后端）
    // ==============================================
    ElMessage.success('下单成功！')
    setTimeout(() => {
      router.push('/user/order')
    }, 500)

    // ==============================================
    // ✅ 真实后端请求版（已写好，以后直接解开注释使用）
    // 需要先在 api/order.ts 定义 createOrder
    // ==============================================
    /*
    // 导入API（解开时要在顶部引入）
    //

    // 构造传给后端的订单数据
    const orderData = {
      serviceName: serviceInfo.value.name,
      serviceDesc: serviceInfo.value.desc,
      price: serviceInfo.value.price,
      address: serviceAddress.value,
      time: selectedTime.value,
      remark: remark.value,
      type: serviceInfo.value.type,
      hospital: hospital.value // 助医专用，其他类型自动为空
    }

    // 发送请求
    const res = await createOrder(orderData)
    ElMessage.success('下单成功！')
    setTimeout(() => {
      router.push('/user/order')
    }, 500)
    */

  } catch (err) {
    console.error('下单失败：', err)
    ElMessage.error('下单失败，请重试')
  }
}
</script>

<style scoped>
.order-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 0;
}
.page-title {
  font-size: 32px;
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
  font-size: 26px;
  font-weight: 600;
  color: #006d5c;
}
.service-desc {
  font-size: 20px;
  color: #666;
  margin-top: 8px;
}
.service-price {
  font-size: 28px;
  color: #f56c6c;
  font-weight: bold;
  margin-top: 12px;
}
.form-section {
  margin-bottom: 30px;
}
.label {
  font-size: 22px;
  color: #333;
  margin-bottom: 12px;
}
.submit-section {
  display: flex;
  justify-content: center;
  gap: 20px;
}
.cancel-btn, .submit-btn {
  font-size: 22px;
  padding: 16px 60px;
}
.picker-wrap {
  width: 100%;
}
.tip-text {
  font-size: 18px;
  color: #e6a23c;
  margin-top: 8px;
}

/* 输入框样式（scoped，只影响当前页面） */
:deep(.el-date-editor.el-input) {
  font-size: 24px !important;
  height: 60px !important;
}
:deep(.el-date-editor .el-input__inner) {
  font-size: 24px !important;
  height: 60px !important;
  line-height: 60px !important;
  padding: 0 24px !important;
}
</style>

<!-- ✅ 加了自定义类名限定，只影响当前页面的时间选择器弹窗，绝对不会污染全局！ -->
<style>
/* 只针对.custom-date-picker类的弹窗生效 */
.custom-date-picker {
  font-size: 24px !important;
  width: 550px !important;
  padding: 20px !important;
}

/* 隐藏Now按钮 */
.custom-date-picker .el-picker-panel__footer .el-picker-panel__link-btn {
  display: none !important;
}

/* 日历格子放大 */
.custom-date-picker .el-date-table td {
  font-size: 26px !important;
  font-weight: 700 !important;
  padding: 18px 0 !important;
  width: 70px !important;
  height: 70px !important;
}

/* 星期表头强制中文 */
.custom-date-picker .el-date-table th:nth-child(1) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(1) span::after { content: '日'; font-size: 24px !important; }
.custom-date-picker .el-date-table th:nth-child(2) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(2) span::after { content: '一'; font-size: 24px !important; }
.custom-date-picker .el-date-table th:nth-child(3) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(3) span::after { content: '二'; font-size: 24px !important; }
.custom-date-picker .el-date-table th:nth-child(4) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(4) span::after { content: '三'; font-size: 24px !important; }
.custom-date-picker .el-date-table th:nth-child(5) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(5) span::after { content: '四'; font-size: 24px !important; }
.custom-date-picker .el-date-table th:nth-child(6) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(6) span::after { content: '五'; font-size: 24px !important; }
.custom-date-picker .el-date-table th:nth-child(7) span { font-size: 0 !important; }
.custom-date-picker .el-date-table th:nth-child(7) span::after { content: '六'; font-size: 24px !important; }

/* 其他元素放大 */
.custom-date-picker .el-date-table th {
  font-size: 24px !important;
  font-weight: bold !important;
  height: 60px !important;
}
.custom-date-picker .el-time-panel .el-time-list li {
  font-size: 26px !important;
  padding: 16px 0 !important;
  height: 60px !important;
  line-height: 60px !important;
}
.custom-date-picker .el-picker-panel__btn {
  font-size: 24px !important;
  padding: 10px 24px !important;
}
.custom-date-picker .el-date-picker__header button {
  width: 48px !important;
  height: 48px !important;
  font-size: 24px !important;
}
.custom-date-picker .el-date-picker__header-label {
  font-size: 28px !important;
  font-weight: bold !important;
}
</style>
