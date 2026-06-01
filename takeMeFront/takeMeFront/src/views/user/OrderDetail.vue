<template>
  <div class="page-container">
    <!-- 页面加载骨架屏 -->
    <div v-if="loading" class="skeleton-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else>
      <div class="header-row">
        <h2 class="page-title">订单详情</h2>
      </div>

      <div class="detail-card">
        <!-- 订单信息（✅ 加安全判断，防止undefined报错） -->
        <div class="detail-section">
          <div class="section-title">订单信息</div>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value">{{ orderInfo?.orderNo || '' }}</span>
            </div>
            <div class="info-item">
              <span class="label">服务状态：</span>
              <el-tag :type="getStatusTagType(orderInfo?.status ?? 0)" size="large">
                {{ getStatusText(orderInfo?.status ?? 0) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value price">¥{{ orderInfo?.totalPrice ?? 0 }}</span>
            </div>
            <div class="info-item">
              <span class="label">下单时间：</span>
              <span class="value">{{ formatTime(orderInfo?.createTime) }}</span>
            </div>
            <div class="info-item">
              <span class="label">服务时间：</span>
              <span class="value">{{ orderInfo?.serviceDate || '' }} {{ orderInfo?.serviceTime || '' }}</span>
            </div>
            <div class="info-item full-width">
              <span class="label">服务地址：</span>
              <span class="value">{{ orderInfo?.address || '' }}</span>
            </div>
          </div>
        </div>

        <!-- 服务项目明细（✅ 加安全判断，防止undefined报错） -->
        <div class="detail-section" v-if="orderInfo?.items && orderInfo.items.length > 0">
          <div class="section-title">服务项目</div>
          <div class="service-items">
            <div class="service-item" v-for="item in orderInfo.items" :key="item.id">
              <div class="item-info">
                <span class="item-name">{{ item.serviceName || '' }}</span>
                <span class="item-price">¥{{ item.servicePrice ?? 0 }} × {{ item.quantity ?? 0 }}</span>
              </div>
              <span class="item-total">¥{{ item.itemPrice ?? 0 }}</span>
            </div>
          </div>
        </div>

        <!-- 订单备注（✅ 加安全判断） -->
        <div class="detail-section">
          <div class="section-title">订单备注</div>
          <p class="remark-text">{{ orderInfo?.remark || '无特殊要求' }}</p>
        </div>

        <!-- 操作按钮（✅ 加安全判断） -->
        <div class="action-section">
          <el-button
            v-if="[0,1].includes(orderInfo?.status ?? -1)"
            type="danger" size="large"
            @click="handleCancelOrder" :loading="cancelLoading"
          >取消订单</el-button>

          <el-button
            v-if="orderInfo?.status === 3"
            type="success" size="large"
            @click="confirmCompleted" :loading="confirmLoading"
          >确认服务完成</el-button>

          <el-button
            v-if="orderInfo?.status === 4 && orderInfo?.isReviewed === 0"
            type="warning" size="large"
            @click="goToReview"
          >评价服务</el-button>
        </div>
      </div>

      <div class="action-buttons">
        <el-button size="large" @click="$router.back()">返回订单列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, cancelOrder, confirmOrder } from '@/api'

const route = useRoute()
const router = useRouter()

// 核心：安全获取订单ID
const getValidOrderId = () => {
  const id = route.params.id
  const numId = Number(id)
  if (isNaN(numId) || numId <= 0) {
    ElMessage.error('订单参数无效！')
    router.back()
    return null
  }
  return numId
}
const orderId = getValidOrderId()

const loading = ref(true)
const cancelLoading = ref(false)
const confirmLoading = ref(false)

// 初始化时确保orderInfo是对象，不会undefined
const orderInfo = ref<any>({
  orderNo: '',
  status: 0,
  totalPrice: 0,
  createTime: '',
  serviceDate: '',
  serviceTime: '',
  address: '',
  remark: '',
  items: [],
  isReviewed: 0
})

// 状态映射
const statusMap = {0:'待接单',1:'已接单',2:'服务中',3:'待确认',4:'已完成',5:'已取消'}
const getStatusText = (s: number) => statusMap[s] || '未知状态'
const getStatusTagType = (s: number) => {
  const m = {0:'warning',1:'primary',2:'primary',3:'info',4:'success',5:'danger'}
  return m[s] || 'info'
}

// 时间格式化
const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

// ✅ 修复：数据赋值逻辑，兼容所有后端返回结构
const fetchOrderDetail = async () => {
  if (!orderId) {
    loading.value = false
    return
  }

  try {
    const res = await getOrderDetail(orderId)
    console.log('订单详情接口返回:', res)

    // 关键：兼容两种返回结构（你的request封装可能剥了一层data）
    // 情况1：res.data是Result<OrderVO>，需要取res.data.data
    // 情况2：res直接是OrderVO，直接赋值
    const data = res?.data?.data || res?.data || res || {}
    orderInfo.value = {
      orderNo: '',
      status: 0,
      totalPrice: 0,
      createTime: '',
      serviceDate: '',
      serviceTime: '',
      address: '',
      remark: '',
      items: [],
      isReviewed: 0,
      ...data
    }
  } catch (e) {
    console.error('获取订单详情失败', e)
    ElMessage.error('获取详情失败')
    // 即使请求失败，也保留初始数据，防止undefined报错
    orderInfo.value = {
      orderNo: '',
      status: 0,
      totalPrice: 0,
      createTime: '',
      serviceDate: '',
      serviceTime: '',
      address: '',
      remark: '',
      items: [],
      isReviewed: 0
    }
  } finally {
    loading.value = false
  }
}

// 取消订单
const handleCancelOrder = async () => {
  if (!orderId) return
  await ElMessageBox.confirm('确定取消订单？','提示',{type:'warning'})
  cancelLoading.value = true
  try {
    await cancelOrder(orderId)
    orderInfo.value.status = 5
    ElMessage.success('取消成功')
  } catch (e) {
    ElMessage.error('取消失败')
  } finally {
    cancelLoading.value = false
  }
}

// 确认完成
const confirmCompleted = async () => {
  if (!orderId) return
  await ElMessageBox.confirm('确认服务已完成？','提示',{type:'success'})
  confirmLoading.value = true
  try {
    await confirmOrder(orderId)
    orderInfo.value.status = 4
    ElMessage.success('已确认完成')
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    confirmLoading.value = false
  }
}

// 去评价
const goToReview = () => {
  if (!orderId) return
  router.push(`/user/order/review/${orderId}`)
}

onMounted(() => {
  fetchOrderDetail()
})

interface OrderItem {
  id: number
  orderId: number
  serviceId: number
  serviceName: string
  servicePrice: number
  quantity: number
  itemPrice: number
  createTime: string
}
</script>

<style scoped>
.page-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}
.skeleton-container { padding: 40px; background: #fff; border-radius: 16px; }
.header-row { margin-bottom: 24px; text-align: center; }
.page-title { font-size: 32px; font-weight: bold; color: #333; }
.detail-card {
  background: #fff; border-radius: 16px; padding: 40px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
}
.detail-section {
  margin-bottom: 32px; padding-bottom: 32px; border-bottom: 1px solid #eee;
}
.detail-section:last-child { border: none; }
.section-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #333; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-item.full-width { grid-column: 1/-1; }
.info-item { display: flex; align-items: center; }
.label { width: 100px; font-size: 18px; color: #666; }
.value { font-size: 18px; color: #333; }
.value.price { color: #f5222d; font-weight: bold; font-size: 20px; }

.service-items { display: flex; flex-direction: column; gap: 12px; }
.service-item {
  padding: 16px; background: #f8faf9; border-radius: 8px;
  display: flex; justify-content: space-between; align-items: center;
}
.item-info { display: flex; flex-direction: column; }
.item-name { font-size: 18px; font-weight: 500; }
.item-price { font-size: 16px; color: #666; }
.item-total { font-size: 18px; color: #f56c6c; font-weight: bold; }

.remark-text {
  padding: 16px; background: #f8faf9; border-radius: 8px;
  font-size: 18px; line-height: 1.6;
}
.action-section { text-align: center; margin-top: 20px; gap: 20px; display: flex; justify-content: center; }
.action-buttons { text-align: center; margin-top: 12px; }
:deep(.el-button) { font-size: 18px !important; padding: 12px 32px !important; }
</style>
