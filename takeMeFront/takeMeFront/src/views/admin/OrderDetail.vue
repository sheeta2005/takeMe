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
          <el-descriptions-item label="用户ID">
            {{ order.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="志愿者ID">
            {{ order.volunteerId || '未分配' }}
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
                <span class="service-name">{{ row.serviceName }}</span>
                <el-link
                  v-if="row.volunteerId"
                  type="success"
                  :underline="false"
                  class="volunteer-link"
                  @click="showVolunteerDetail(row.volunteerId)"
                >
                  <el-icon><User /></el-icon>
                  查看志愿者
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
            <span class="total-value">¥{{ order.totalAmount || order.totalPrice }}</span>
          </div>
        </div>
      </el-card>

      <div class="action-section">
        <el-button
          v-if="[0, 1, 2].includes(order.status)"
          type="success"
          size="large"
          @click="handleComplete"
          :loading="completeLoading"
        >
          <el-icon><CircleCheck /></el-icon>
          标记完成
        </el-button>

        <el-button
          v-if="order.status !== 4 && order.status !== 5 && order.status !== 6"
          type="danger"
          size="large"
          @click="handleCancel"
          :loading="cancelLoading"
        >
          <el-icon><CloseBold /></el-icon>
          取消订单
        </el-button>
      </div>

      <div class="back-section">
        <el-button size="large" @click="back">
          <el-icon><Back /></el-icon>
          返回订单列表
        </el-button>
      </div>
    </template>

    <el-empty v-else description="订单不存在" :image-size="200" />

    <el-dialog
      v-model="volunteerDetailVisible"
      title="志愿者信息"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="volunteerDetail" class="volunteer-detail">
        <div class="volunteer-header">
          <el-avatar :size="80" :src="volunteerDetail.avatar || defaultAvatar" />
          <div class="volunteer-info">
            <h3 class="volunteer-name">{{ volunteerDetail.realName }}</h3>
            <div class="volunteer-stats">
              <el-tag type="success" size="large">
                完成服务 {{ volunteerDetail.completedCount }} 次
              </el-tag>
              <el-tag type="warning" size="large">
                平均评分 {{ volunteerDetail.averageRating }} 分
              </el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="联系电话">
            {{ volunteerDetail.volunteer?.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ volunteerDetail.volunteer?.gender === 1 ? '男' : volunteerDetail.volunteer?.gender === 2 ? '女' : '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ volunteerDetail.volunteer?.age || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="地址">
            {{ volunteerDetail.volunteer?.address || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="服务总时长">
            {{ volunteerDetail.volunteer?.totalServiceHours || 0 }} 小时
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="reviews-section">
          <h4 class="section-title">
            <el-icon><ChatDotRound /></el-icon>
            评价内容（{{ volunteerDetail.reviews?.length || 0 }} 条）
          </h4>
          <div v-if="volunteerDetail.reviews && volunteerDetail.reviews.length > 0" class="reviews-list">
            <div v-for="review in volunteerDetail.reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <el-avatar :size="40" :src="review.userAvatar || defaultAvatar" />
                <div class="review-info">
                  <div class="review-user">{{ review.userName }}</div>
                  <div class="review-meta">
                    <el-rate :model-value="review.rating" disabled size="small" />
                    <span class="review-time">{{ review.createTime }}</span>
                  </div>
                </div>
              </div>
              <div class="review-content">{{ review.content }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无评价" :image-size="80" />
        </div>
      </div>

      <template #footer>
        <el-button size="large" @click="volunteerDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, List, Location, CircleCheck, CloseBold, Back, User, ChatDotRound
} from '@element-plus/icons-vue'
import { getOrderDetail, completeOrder, cancelOrder } from '@/api/admin'
import { getVolunteerDetail } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const completeLoading = ref(false)
const cancelLoading = ref(false)
const order = ref<any>(null)
const volunteerDetailVisible = ref(false)
const volunteerDetail = ref<any>(null)
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNlNWU3ZWIiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk0YTNiOCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWksei0pTwvdGV4dD48L3N2Zz4='

const serviceTypeMap: Record<number, string> = {
  0: '代购服务',
  1: '助洁服务',
  2: '助餐服务',
  3: '助医服务',
  4: '陪伴服务'
}

const serviceTypeTagMap: Record<number, string> = {
  0: 'primary',
  1: 'success',
  2: 'warning',
  3: 'danger',
  4: 'info'
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'warning',
    4: 'success',
    5: 'danger',
    6: 'danger'
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
    const res = await getOrderDetail(id)
    if (res.code === 200) {
      order.value = res.data || {}
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载订单详情失败')
  } finally {
    loading.value = false
  }
}

const showVolunteerDetail = async (volunteerId: number) => {
  try {
    const res = await getVolunteerDetail(volunteerId)
    if (res.code === 200) {
      volunteerDetail.value = res.data
      volunteerDetailVisible.value = true
    } else {
      ElMessage.error('加载志愿者信息失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载志愿者信息失败')
  }
}

const handleComplete = async () => {
  if (!order.value?.id) return

  try {
    await ElMessageBox.confirm('确定完成该订单吗？', '提示', { type: 'warning' })
    completeLoading.value = true
    await completeOrder(order.value.id)
    ElMessage.success('订单已完成')
    await loadOrderDetail(order.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    completeLoading.value = false
  }
}

const handleCancel = async () => {
  if (!order.value?.id) return

  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    cancelLoading.value = true
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await loadOrderDetail(order.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    cancelLoading.value = false
  }
}

const back = () => {
  router.back()
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
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 0;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
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
  color: #1e293b;
}

.items-count {
  margin-left: auto;
}

.address-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.address-content .el-icon {
  color: #00a88d;
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

.volunteer-link {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
}

.price-cell {
  color: #64748b;
  font-weight: 500;
}

.subtotal-cell {
  color: #ef4444;
  font-weight: bold;
  font-size: 16px;
}

.total-summary {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid #00a88d;
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
  color: #64748b;
}

.total-value {
  font-size: 28px;
  font-weight: bold;
  color: #ef4444;
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

.volunteer-detail {
  padding: 0 8px;
}

.volunteer-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.volunteer-info {
  flex: 1;
}

.volunteer-name {
  margin: 0 0 12px 0;
  font-size: 22px;
  font-weight: bold;
  color: #1e293b;
}

.volunteer-stats {
  display: flex;
  gap: 12px;
}

.reviews-section {
  margin-top: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.review-info {
  flex: 1;
}

.review-user {
  font-weight: 600;
  color: #1e293b;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.review-time {
  font-size: 12px;
  color: #94a3b8;
}

.review-content {
  color: #475569;
  line-height: 1.6;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  width: 120px;
}
</style>
