<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的待办</h2>
      <p class="page-subtitle">管理您的服务订单</p>
    </div>

    <el-skeleton :rows="3" animated v-if="loading" />

    <template v-else>
      <el-empty v-if="!currentOrder && availableOrders.length === 0" description="暂无待办任务" :image-size="200">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </el-empty>

      <template v-else>
        <el-alert
          v-if="currentOrder"
          title="您有一个进行中的订单，请先完成该订单"
          type="warning"
          :closable="false"
          show-icon
          class="notice-alert"
        />

        <div v-if="currentOrder" class="current-order-section">
          <h3 class="section-title">
            <el-icon><Clock /></el-icon>
            进行中的订单
          </h3>
          <el-card class="order-card current-order-card" shadow="hover">
            <div class="order-card-header">
              <div class="order-info">
                <span class="order-label">订单编号</span>
                <span class="order-no">{{ currentOrder.orderNo }}</span>
              </div>
              <el-tag type="primary" size="large" effect="dark">
                <el-icon><Loading /></el-icon>
                服务中
              </el-tag>
            </div>

            <el-divider />

            <div class="order-card-body">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="服务类型">
                  <el-tag :type="getServiceTypeTag(currentOrder.serviceType)">
                    {{ getServiceTypeName(currentOrder.serviceType) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="服务时间">
                  <div class="time-info">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ currentOrder.serviceDate }} {{ currentOrder.serviceTime }}</span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="服务地址">
                  <div class="address-info">
                    <el-icon><Location /></el-icon>
                    <span>{{ currentOrder.address }}</span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="备注" v-if="currentOrder.remark">
                  <el-alert :title="currentOrder.remark" type="info" :closable="false" show-icon />
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="order-card-footer">
              <div class="price-info">
                <span class="price-label">订单金额：</span>
                <span class="price-value">¥{{ currentOrder.totalPrice }}</span>
              </div>
              <div class="action-buttons">
                <el-button type="primary" size="large" @click="goToOrderDetail(currentOrder.id)">
                  <el-icon><View /></el-icon>
                  查看详情
                </el-button>
                <el-button type="success" size="large" @click="completeOrder(currentOrder)">
                  <el-icon><CircleCheck /></el-icon>
                  完成服务
                </el-button>
                <el-button type="danger" size="large" @click="abandonOrder(currentOrder)">
                  <el-icon><CloseBold /></el-icon>
                  放弃订单
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <div v-if="availableOrders.length > 0" class="available-orders-section">
          <h3 class="section-title">
            <el-icon><List /></el-icon>
            待接单任务
            <el-badge :value="availableOrders.length" :max="99" class="badge" />
          </h3>
          <div class="order-grid">
            <el-card
              v-for="order in availableOrders"
              :key="order.id"
              class="order-card available-order-card"
              shadow="hover"
            >
              <div class="order-card-header">
                <div class="order-info">
                  <span class="order-label">订单编号</span>
                  <span class="order-no">{{ order.orderNo }}</span>
                </div>
                <el-tag type="warning" size="large">
                  <el-icon><Bell /></el-icon>
                  待确认
                </el-tag>
              </div>

              <el-divider />

              <div class="order-card-body">
                <div class="info-item">
                  <el-icon class="info-icon"><Ticket /></el-icon>
                  <span class="info-label">服务类型：</span>
                  <el-tag :type="getServiceTypeTag(order.serviceType)" size="small">
                    {{ getServiceTypeName(order.serviceType) }}
                  </el-tag>
                </div>
                <div class="info-item">
                  <el-icon class="info-icon"><Calendar /></el-icon>
                  <span class="info-label">服务时间：</span>
                  <span class="info-value">{{ order.serviceDate }} {{ order.serviceTime }}</span>
                </div>
                <div class="info-item">
                  <el-icon class="info-icon"><Location /></el-icon>
                  <span class="info-label">服务地址：</span>
                  <span class="info-value">{{ order.address }}</span>
                </div>
                <div class="info-item" v-if="order.remark">
                  <el-icon class="info-icon"><Document /></el-icon>
                  <span class="info-label">备注：</span>
                  <span class="info-value">{{ order.remark }}</span>
                </div>
              </div>

              <div class="order-card-footer">
                <div class="price-info">
                  <span class="price-label">订单金额：</span>
                  <span class="price-value">¥{{ order.totalPrice }}</span>
                </div>
                <div class="action-buttons">
                  <el-button type="primary" size="large" @click="confirmOrder(order)">
                    <el-icon><Check /></el-icon>
                    确认接单
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, Clock, Loading, Calendar, Location, Document, View,
  CircleCheck, CloseBold, List, Bell, Ticket, Check
} from '@element-plus/icons-vue'
import {
  getVolunteerOrderList,
  confirmOrder as apiConfirmOrder,
  completeOrder as apiCompleteOrder,
  abandonOrder as apiAbandonOrder
} from '@/api/volunteer'

const router = useRouter()

const loading = ref(true)
const orderList = ref<any[]>([])

const currentOrder = computed(() => {
  return orderList.value.find(o => o.status === 2) || null
})

const availableOrders = computed(() => {
  return orderList.value.filter(o => o.status === 1).slice(0, 3)
})

const serviceTypeMap: Record<number, string> = {
  0: '代购服务',
  1: '助洁服务',
  2: '助餐服务',
  3: '助医服务',
  4: '陪伴服务'
}

const serviceTypeTagMap: Record<number, string> = {
  0: 'info',
  1: 'primary',
  2: 'success',
  3: 'warning',
  4: 'danger'
}

const getServiceTypeName = (type: number) => {
  return serviceTypeMap[type] || '未知'
}

const getServiceTypeTag = (type: number) => {
  return serviceTypeTagMap[type] || 'info'
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getVolunteerOrderList({
      page: 1,
      pageSize: 20
    })
    if (res.code === 200) {
      orderList.value = res.data?.records || []
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadOrders()
}

const goToOrderDetail = (orderId: number) => {
  router.push(`/volunteer/order/${orderId}`)
}

const confirmOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm(
      `确认接取订单 ${order.orderNo}？接单后请及时完成服务。`,
      '确认接单',
      {
        confirmButtonText: '确认接单',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await apiConfirmOrder(order.id)
    ElMessage.success('接单成功')
    await loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '接单失败')
    }
  }
}

const completeOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm(
      `确认已完成订单 ${order.orderNo} 的服务？完成后将获得相应积分。`,
      '完成服务',
      {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await apiCompleteOrder(order.id)
    ElMessage.success('订单已完成，积分已发放')
    await loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '完成订单失败')
    }
  }
}

const abandonOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要放弃订单 ${order.orderNo} 吗？放弃订单可能会影响您的信用评分。`,
      '放弃订单',
      {
        confirmButtonText: '确定放弃',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await apiAbandonOrder(order.id)
    ElMessage.warning('订单已放弃')
    await loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '放弃订单失败')
    }
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 0;
}

.page-header {
  margin-bottom: 32px;
  text-align: center;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.notice-alert {
  margin-bottom: 24px;
  border-radius: 8px;
}

.current-order-section,
.available-orders-section {
  margin-bottom: 40px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.badge {
  margin-left: 8px;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.order-card {
  border-radius: 12px;
  transition: all 0.3s ease;
}

.order-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 184, 153, 0.15);
}

.current-order-card {
  border: 2px solid #00b899;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-label {
  font-size: 12px;
  color: #999;
}

.order-no {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  font-family: 'Courier New', monospace;
}

.order-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-icon {
  color: #00b899;
  font-size: 18px;
}

.info-label {
  font-size: 14px;
  color: #666;
  min-width: 80px;
}

.info-value {
  font-size: 14px;
  color: #333;
}

.time-info,
.address-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-info .el-icon,
.address-info .el-icon {
  color: #00b899;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-label {
  font-size: 14px;
  color: #666;
}

.price-value {
  font-size: 24px;
  font-weight: bold;
  color: #f5222d;
}

.action-buttons {
  display: flex;
  gap: 12px;
}
</style>
