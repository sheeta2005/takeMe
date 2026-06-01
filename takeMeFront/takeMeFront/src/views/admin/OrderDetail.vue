<template>
  <div class="detail-container">
    <div class="header-row">
      <h2 class="page-title">订单详情</h2>
      <el-button @click="$router.back()">返回列表</el-button>
    </div>

    <div class="detail-card">
      <!-- 订单基础信息 -->
      <div class="detail-section">
        <h3 class="section-title">订单信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">订单号：</span>
            <span class="value">{{ orderDetail.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">服务类型：</span>
            <el-tag v-if="orderItems.length > 0" :type="getServiceTypeTagType(orderItems[0].serviceType)">
              {{ getServiceTypeText(orderItems[0].serviceType) }}
            </el-tag>
            <el-tag v-else>未知</el-tag>
          </div>
          <div class="info-item">
            <span class="label">订单状态：</span>
            <el-tag :type="getOrderStatusTagType(orderDetail.status)" size="large">
              {{ getOrderStatusText(orderDetail.status) }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="label">下单时间：</span>
            <span class="value">{{ formatDateTime(orderDetail.createTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">服务时间：</span>
            <span class="value">{{ orderDetail.serviceDate }} {{ orderDetail.serviceTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">订单金额：</span>
            <span class="value price">¥{{ orderDetail.totalPrice }}</span>
          </div>
        </div>
      </div>

      <!-- 服务地址信息 -->
      <div class="detail-section">
        <h3 class="section-title">服务地址</h3>
        <div class="address-info">
          <el-icon><Location /></el-icon>
          <span>{{ orderDetail.address }}</span>
        </div>
      </div>

      <!-- 服务项列表 -->
      <div class="detail-section" v-if="orderItems.length > 0">
        <h3 class="section-title">服务项目</h3>
        <div class="service-items">
          <div class="service-item" v-for="item in orderItems" :key="item.id">
            <div class="item-info">
              <span class="item-name">{{ item.serviceName }}</span>
              <span class="item-price">¥{{ item.servicePrice }} × {{ item.quantity }}</span>
            </div>
            <span class="item-total">¥{{ item.itemPrice }}</span>
          </div>
        </div>
      </div>

      <!-- 备注信息 -->
      <div class="detail-section" v-if="orderDetail.remark">
        <h3 class="section-title">订单备注</h3>
        <div class="remark-info">
          {{ orderDetail.remark }}
        </div>
      </div>

      <!-- 完成时间（仅已完成订单显示） -->
      <div class="detail-section" v-if="orderDetail.completeTime">
        <h3 class="section-title">完成信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">完成时间：</span>
            <span class="value">{{ formatDateTime(orderDetail.completeTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">评价状态：</span>
            <el-tag :type="orderDetail.isReviewed === 1 ? 'success' : 'info'">
              {{ orderDetail.isReviewed === 1 ? '已评价' : '未评价' }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { getOrderDetail } from '@/api'

const route = useRoute()
const router = useRouter()

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

const orderDetail = ref<any>({})
const orderItems = ref<OrderItem[]>([])

onMounted(() => {
  // ✅ 修复1：用 params 取路由参数
  const orderId = route.params.id as string
  if (orderId) {
    fetchOrderDetail(Number(orderId))
  } else {
    ElMessage.error('订单ID无效')
    router.back()
  }
})

const fetchOrderDetail = async (id: number) => {
  try {
    const res = await getOrderDetail(id)
    // ✅ 修复2：取后端返回的 data 字段
    orderDetail.value = res.data.data
    orderItems.value = res.data.data.items || []
  } catch (err) {
    console.error('获取订单详情失败', err)
    ElMessage.error('获取订单详情失败')
  }
}

// 格式化日期时间
const formatDateTime = (dateTime: string | null) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 服务类型映射（对应ServicePackage的type字段）
const getServiceTypeText = (type: number) => {
  const map: Record<number, string> = {
    0: '代购服务',
    1: '助洁服务',
    2: '助餐服务',
    3: '助医服务',
    4: '陪伴服务'
  }
  return map[type] || '未知'
}

const getServiceTypeTagType = (type: number) => {
  const map: Record<number, string> = {
    0: 'success',    // 代购
    1: 'info',       // 助洁
    2: 'warning',    // 助餐
    3: 'danger',     // 助医
    4: 'primary'     // 陪伴
  }
  return map[type] || 'info'
}

// 订单状态映射（需要根据您的业务逻辑调整）
const getOrderStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待接单',
    1: '服务中',
    2: '已完成',
    3: '已取消'
  }
  return map[status] || '未知'
}

const getOrderStatusTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',   // 待接单
    1: 'primary',   // 服务中
    2: 'success',   // 已完成
    3: 'danger'     // 已取消
  }
  return map[status] || 'info'
}
</script>

<style scoped>
/* 样式部分无需修改 */
.detail-container {
  width: 100%;
  padding: 10px 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 30px;
}

.detail-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.value {
  font-size: 14px;
  color: #333;
}

.price {
  color: #f56c6c;
  font-weight: 600;
  font-size: 16px;
}

.address-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.service-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.item-info {
  display: flex;
  justify-content: space-between;
  flex: 1;
  margin-right: 20px;
}

.item-name {
  font-size: 14px;
  color: #333;
}

.item-price {
  font-size: 14px;
  color: #909399;
}

.item-total {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 600;
  white-space: nowrap;
}

.remark-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}
</style>
