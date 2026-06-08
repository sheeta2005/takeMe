<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">订单详情</h2>
      <p class="page-subtitle">查看订单详细信息</p>
    </div>

    <el-skeleton :rows="8" animated v-if="loading" />

    <template v-else-if="order">
      <el-card class="detail-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Document /></el-icon>
            <span>订单信息</span>
            <el-tag :type="getStatusType(order.status)" size="large" effect="dark">
              {{ getStatusText(order.status) }}
            </el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">
            <span class="order-no-text">{{ order.orderNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ formatTime(order.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="服务时间" v-if="order.serviceDate">
            {{ order.serviceDate }} {{ order.serviceTime || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="服务地址" v-if="order.address">
            <div class="address-content">
              <el-icon><Location /></el-icon>
              {{ order.address }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="联系人" v-if="order.contactName">
            {{ order.contactName }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话" v-if="order.contactPhone">
            {{ order.contactPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" v-if="order.remark" :span="2">
            {{ order.remark }}
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
                <el-link
                  type="primary"
                  :underline="false"
                  class="service-name-link"
                  @click="showServiceDetail(row)"
                >
                  {{ row.serviceName }}
                </el-link>
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

    <el-dialog
      v-model="serviceDetailVisible"
      title="服务详情"
      width="700px"
      :close-on-click-modal="false"
      class="service-detail-dialog"
    >
      <div v-if="currentService" class="detail-content">
        <div class="detail-section">
          <div class="section-title">
            <el-icon><Document /></el-icon>
            <span>服务信息</span>
          </div>
          <el-descriptions :column="2" border class="detail-descriptions">
            <el-descriptions-item label="服务类型">
              <el-tag :type="getServiceTypeTag(currentService.serviceType)" size="large">
                {{ getServiceTypeName(currentService.serviceType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="服务名称">
              <span style="font-weight: 500; font-size: 16px">{{ currentService.serviceName }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="服务单价">
              <span style="color: #f5222d; font-weight: bold; font-size: 18px">¥{{ currentService.servicePrice }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="数量">
              <el-tag type="primary" size="large">×{{ currentService.quantity }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="小计金额" :span="2">
              <span style="color: #f5222d; font-weight: bold; font-size: 20px">¥{{ currentService.itemPrice }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section" v-if="currentService.serviceDate || currentService.serviceTime">
          <div class="section-title">
            <el-icon><Calendar /></el-icon>
            <span>预约时间</span>
          </div>
          <el-descriptions :column="2" border class="detail-descriptions">
            <el-descriptions-item label="服务日期" v-if="currentService.serviceDate">
              <el-tag type="success" size="large">
                <el-icon style="margin-right: 4px"><Clock /></el-icon>
                {{ currentService.serviceDate }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="服务时间" v-if="currentService.serviceTime">
              <el-tag type="warning" size="large">
                <el-icon style="margin-right: 4px"><Timer /></el-icon>
                {{ currentService.serviceTime }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section" v-if="currentService.address">
          <div class="section-title">
            <el-icon><Location /></el-icon>
            <span>服务地址</span>
          </div>
          <div class="address-box">
            <el-icon class="address-icon"><LocationFilled /></el-icon>
            <span class="address-text">{{ currentService.address }}</span>
          </div>
        </div>

        <div class="detail-section" v-if="currentService.remark">
          <div class="section-title">
            <el-icon><EditPen /></el-icon>
            <span>备注信息</span>
          </div>
          <div class="remark-box">
            {{ currentService.remark }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button size="large" @click="serviceDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, List, Location, Coin, CircleCheck, CloseBold, Back,
  Calendar, Clock, EditPen, Timer, LocationFilled
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

const serviceDetailVisible = ref(false)
const currentService = ref<any>(null)

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

const showServiceDetail = (service: any) => {
  currentService.value = service
  serviceDetailVisible.value = true
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

.service-name-link {
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.service-name-link:hover {
  opacity: 0.8;
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

/* 服务详情弹窗样式 */
.service-detail-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #00b899 0%, #00d4a8 100%);
  color: white;
  padding: 20px 24px;
  margin: 0;
}

.service-detail-dialog :deep(.el-dialog__title) {
  color: white;
  font-size: 20px;
  font-weight: 600;
}

.service-detail-dialog :deep(.el-dialog__close) {
  color: white;
}

.service-detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 600px;
  overflow-y: auto;
}

.detail-content {
  padding: 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 4px solid #00b899;
}

.section-title .el-icon {
  font-size: 20px;
  color: #00b899;
}

.detail-descriptions {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.detail-descriptions .el-descriptions__label) {
  background-color: #fafafa;
  font-weight: 600;
  width: 120px;
}

:deep(.detail-descriptions .el-descriptions__content) {
  padding: 12px 16px;
}

.address-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f0f9f4;
  border-radius: 8px;
  border: 1px solid #b7eb8f;
}

.address-icon {
  font-size: 24px;
  color: #00b899;
  flex-shrink: 0;
}

.address-text {
  font-size: 16px;
  color: #333;
  line-height: 1.6;
  word-break: break-word;
}

.remark-box {
  padding: 16px 20px;
  background: #fff7e6;
  border-radius: 8px;
  border: 1px solid #ffd591;
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.service-detail-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
  background-color: #fafafa;
}
</style>

