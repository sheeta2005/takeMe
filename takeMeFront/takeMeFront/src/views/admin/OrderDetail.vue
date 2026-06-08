<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">订单详情</h2>
          <p class="page-subtitle">查看订单详细信息</p>
        </div>
        <el-button size="large" @click="$router.back()">
          <el-icon><Back /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <!-- 订单基础信息 -->
    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Document /></el-icon>
            <span class="card-title">订单信息</span>
          </div>
          <el-tag :type="getOrderStatusTagType(orderDetail.status)" size="large">
            {{ getOrderStatusText(orderDetail.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border size="large">
        <el-descriptions-item label="订单号">
          <span class="value-text">{{ orderDetail.orderNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="服务类型">
          <el-tag v-if="orderItems.length > 0" :type="getServiceTypeTagType(orderItems[0].serviceType)">
            {{ getServiceTypeText(orderItems[0].serviceType) }}
          </el-tag>
          <el-tag v-else>未知</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          <span class="time-text">{{ formatDateTime(orderDetail.createTime) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="服务时间">
          <span class="value-text">{{ orderDetail.serviceDate }} {{ orderDetail.serviceTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单金额">
          <span class="price-text">¥{{ orderDetail.totalPrice }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 服务地址 -->
    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Location /></el-icon>
            <span class="card-title">服务地址</span>
          </div>
        </div>
      </template>

      <div class="address-box">
        <el-icon :size="18" color="#00a88d"><Location /></el-icon>
        <span class="address-text">{{ orderDetail.address }}</span>
      </div>
    </el-card>

    <!-- 服务项目列表 -->
    <el-card class="detail-card" shadow="hover" v-if="orderItems.length > 0">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><List /></el-icon>
            <span class="card-title">服务项目</span>
          </div>
        </div>
      </template>

      <el-table :data="orderItems" stripe style="width: 100%">
        <el-table-column prop="serviceName" label="服务名称" min-width="200" />
        <el-table-column prop="servicePrice" label="单价" width="120">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.servicePrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.itemPrice }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 备注信息 -->
    <el-card class="detail-card" shadow="hover" v-if="orderDetail.remark">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><ChatLineRound /></el-icon>
            <span class="card-title">订单备注</span>
          </div>
        </div>
      </template>

      <div class="remark-box">
        {{ orderDetail.remark }}
      </div>
    </el-card>

    <!-- 完成信息 -->
    <el-card class="detail-card" shadow="hover" v-if="orderDetail.completeTime">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><CircleCheck /></el-icon>
            <span class="card-title">完成信息</span>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border size="large">
        <el-descriptions-item label="完成时间">
          <span class="time-text">{{ formatDateTime(orderDetail.completeTime) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="评价状态">
          <el-tag :type="orderDetail.isReviewed === 1 ? 'success' : 'info'">
            {{ orderDetail.isReviewed === 1 ? '已评价' : '未评价' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Document, Location, List, ChatLineRound, CircleCheck } from '@element-plus/icons-vue'
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
    orderDetail.value = res.data.data
    orderItems.value = res.data.data.items || []
  } catch (err) {
    console.error('获取订单详情失败', err)
    ElMessage.error('获取订单详情失败')
  }
}

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
    0: 'success',
    1: 'info',
    2: 'warning',
    3: 'danger',
    4: 'primary'
  }
  return map[type] || 'info'
}

const getOrderStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待接取',
    1: '服务中',
    2: '已完成',
    3: '已取消'
  }
  return map[status] || '未知'
}

const getOrderStatusTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'danger'
  }
  return map[status] || 'info'
}
</script>

<style scoped>
.detail-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.value-text {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.time-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.price-text {
  color: var(--danger-color);
  font-weight: 600;
  font-size: 16px;
}

.address-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.address-text {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.6;
}

.remark-box {
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}
</style>
