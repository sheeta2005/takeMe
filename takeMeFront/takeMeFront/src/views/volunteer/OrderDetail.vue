<template>
  <div class="page-container">
    <el-skeleton :rows="8" animated v-if="loading" />

    <template v-else-if="order">
      <div class="page-header">
        <h2 class="page-title">订单详情</h2>
        <el-tag :type="getStatusType(order.status)" size="large" effect="dark">
          {{ getStatusText(order.status) }}
        </el-tag>
      </div>

      <el-card class="detail-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Document /></el-icon>
            <span>订单信息</span>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">
            <el-tag type="info" size="large">{{ order.orderNo }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(order.status)" size="large">
              {{ getStatusText(order.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="price-text">¥{{ order.totalPrice }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ formatTime(order.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="服务时间">
            {{ order.serviceDate }} {{ order.serviceTime }}
          </el-descriptions-item>
          <el-descriptions-item label="服务地址" :span="2">
            <div class="address-content">
              <el-icon><Location /></el-icon>
              <span>{{ order.address }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2" v-if="order.remark">
            <el-alert
              :title="order.remark"
              type="info"
              :closable="false"
              show-icon
            />
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="detail-card" shadow="hover" v-if="order.items && order.items.length > 0">
        <template #header>
          <div class="card-header">
            <el-icon><List /></el-icon>
            <span>服务项目明细</span>
            <el-tag type="info" size="small" class="items-count">
              共 {{ order.items.length }} 项
            </el-tag>
          </div>
        </template>

        <el-table :data="order.items" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="serviceName" label="服务名称" min-width="200">
            <template #default="{ row }">
              <div class="service-name-cell">
                <el-tag :type="getServiceTypeTag(row.serviceType)" size="small">
                  {{ getServiceTypeName(row.serviceType) }}
                </el-tag>
                <span class="service-name">{{ row.serviceName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="servicePrice" label="单价" width="120" align="right">
            <template #default="{ row }">
              <span class="price-cell">¥{{ row.servicePrice }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="primary" size="small">×{{ row.quantity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120" align="right">
            <template #default="{ row }">
              <span class="subtotal-cell">¥{{ row.itemPrice }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="total-summary">
          <div class="total-row">
            <span class="total-label">订单总计：</span>
            <span class="total-value">¥{{ order.totalPrice }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="detail-card" shadow="hover" v-if="order.status === 4">
        <template #header>
          <div class="card-header">
            <el-icon><Coin /></el-icon>
            <span>积分信息</span>
          </div>
        </template>

        <el-result
          icon="success"
          title="服务已完成"
          :sub-title="`获得 ${order.points || 0} 积分`"
        >
          <template #extra>
            <div class="points-info">
              <el-statistic title="本次获得积分" :value="order.points || 0" suffix="分" />
              <el-statistic title="累计服务时长" :value="volunteerStore.totalServiceHours" suffix="小时" />
            </div>
          </template>
        </el-result>
      </el-card>

      <div class="action-section" v-if="order.status === 2">
        <el-button type="success" size="large" @click="handleCompleteOrder" :loading="completing">
          <el-icon><CircleCheck /></el-icon>
          完成服务
        </el-button>
        <el-button type="danger" size="large" @click="handleAbandonOrder" :loading="abandoning">
          <el-icon><CloseBold /></el-icon>
          放弃订单
        </el-button>
      </div>

      <div class="back-section">
        <el-button size="large" @click="back">
          <el-icon><Back /></el-icon>
          返回上一页
        </el-button>
      </div>
    </template>

    <el-empty v-else description="订单不存在" :image-size="200" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, List, Location, Coin, CircleCheck, CloseBold, Back
} from '@element-plus/icons-vue'
import {
  getVolunteerOrderDetail,
  completeOrder as apiCompleteOrder,
  abandonOrder as apiAbandonOrder
} from '@/api/volunteer'
import { useVolunteerStore } from '@/stores/volunteer'

const route = useRoute()
const router = useRouter()
const volunteerStore = useVolunteerStore()

const loading = ref(true)
const completing = ref(false)
const abandoning = ref(false)
const order = ref<any>(null)

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

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'warning',
    2: 'primary',
    3: 'info',
    4: 'success',
    5: 'danger',
    6: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待接单',
    1: '已接单',
    2: '服务中',
    3: '待确认',
    4: '已完成',
    5: '已取消',
    6: '已放弃'
  }
  return map[status] || '未知'
}

const getServiceTypeName = (type: number) => {
  return serviceTypeMap[type] || '未知'
}

const getServiceTypeTag = (type: number) => {
  return serviceTypeTagMap[type] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadOrderDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getVolunteerOrderDetail(id)
    if (res.code === 200) {
      order.value = res.data || {}
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载订单详情失败')
  } finally {
    loading.value = false
  }
}

const handleCompleteOrder = async () => {
  if (!order.value?.id) return

  try {
    await ElMessageBox.confirm('确认已完成服务？完成后将获得相应积分。', '完成服务', {
      confirmButtonText: '确认完成',
      cancelButtonText: '取消',
      type: 'success'
    })

    completing.value = true
    await apiCompleteOrder(order.value.id)
    ElMessage.success('订单已完成，积分已发放')
    await loadOrderDetail(order.value.id)
    await volunteerStore.fetchVolunteerInfo()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '完成订单失败')
    }
  } finally {
    completing.value = false
  }
}

const handleAbandonOrder = async () => {
  if (!order.value?.id) return

  try {
    await ElMessageBox.confirm('确定要放弃这个订单吗？放弃订单可能会影响您的信用评分。', '放弃订单', {
      confirmButtonText: '确定放弃',
      cancelButtonText: '取消',
      type: 'warning'
    })

    abandoning.value = true
    await apiAbandonOrder(order.value.id)
    ElMessage.warning('订单已放弃')
    await loadOrderDetail(order.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '放弃订单失败')
    }
  } finally {
    abandoning.value = false
  }
}

onMounted(() => {
  const id = route.params.id
  if (id && !isNaN(Number(id))) {
    loadOrderDetail(Number(id))
  } else {
    ElMessage.error('订单参数无效')
    router.back()
  }
})

const back = () => {
  router.back()
}
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.detail-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.items-count {
  margin-left: auto;
}

.price-text {
  font-size: 20px;
  font-weight: bold;
  color: #f5222d;
}

.address-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.address-content .el-icon {
  color: #00b899;
  font-size: 18px;
}

.service-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.service-name {
  font-weight: 500;
}

.price-cell {
  color: #666;
  font-weight: 500;
}

.subtotal-cell {
  color: #f5222d;
  font-weight: bold;
  font-size: 16px;
}

.total-summary {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid #00b899;
  display: flex;
  justify-content: flex-end;
}

.total-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.total-label {
  font-size: 16px;
  color: #666;
}

.total-value {
  font-size: 28px;
  font-weight: bold;
  color: #f5222d;
}

.points-info {
  display: flex;
  gap: 48px;
  justify-content: center;
  margin-top: 16px;
}

.action-section {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin: 32px 0;
}

.back-section {
  text-align: center;
  margin-top: 24px;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  width: 120px;
}

:deep(.el-statistic__content) {
  font-size: 24px;
  font-weight: bold;
  color: #00b899;
}
</style>
