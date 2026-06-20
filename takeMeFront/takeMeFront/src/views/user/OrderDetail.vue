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
                <span v-else class="no-volunteer-tip">
                  <el-tag type="info" size="small">等待接取</el-tag>
                </span>
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

      <div class="action-section">
        <el-button
          v-if="[0, 1].includes(order.status)"
          type="danger"
          size="large"
          @click="handleCancelOrder"
          :loading="cancelLoading"
        >
          <el-icon><CloseBold /></el-icon>
          取消订单
        </el-button>

        <el-button
          v-if="order.status === 3"
          type="success"
          size="large"
          @click="handleConfirmOrder"
          :loading="confirmLoading"
        >
          <el-icon><CircleCheck /></el-icon>
          确认服务完成
        </el-button>

        <el-button
          v-if="order.status === 4 && order.isReviewed === 0"
          type="warning"
          size="large"
          @click="goToReview"
        >
          <el-icon><Star /></el-icon>
          评价服务
        </el-button>

        <el-button
          v-if="order.status === 4 && order.isReviewed === 1"
          type="info"
          size="large"
          @click="goToReview"
        >
          <el-icon><Edit /></el-icon>
          修改评价
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
      v-model="serviceDetailVisible"
      :title="currentService?.serviceName || '服务详情'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-descriptions :column="1" border v-if="currentService">
        <el-descriptions-item label="服务类型">
          <el-tag :type="getServiceTypeTag(currentService.serviceType)">
            {{ getServiceTypeName(currentService.serviceType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务名称">
          {{ currentService.serviceName }}
        </el-descriptions-item>
        <el-descriptions-item label="服务单价">
          <span class="price-text">¥{{ currentService.servicePrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="数量">
          {{ currentService.quantity }}
        </el-descriptions-item>
        <el-descriptions-item label="小计金额">
          <span class="price-text">¥{{ currentService.itemPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="服务时间" v-if="currentService.serviceDate">
          {{ currentService.serviceDate }} {{ currentService.serviceTime || '' }}
        </el-descriptions-item>
        <el-descriptions-item label="服务地址" v-if="currentService.address">
          {{ currentService.address }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" v-if="currentService.remark">
          {{ currentService.remark }}
        </el-descriptions-item>
        <el-descriptions-item label="志愿者" v-if="currentService.volunteerId">
          <el-link type="primary" @click="showVolunteerDetail(currentService.volunteerId)">
            查看志愿者信息
          </el-link>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button size="large" @click="serviceDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="volunteerDetailVisible"
      title="志愿者信息"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="volunteerDetail && volunteerDetail.volunteer" class="volunteer-detail">
        <div class="volunteer-header">
          <el-avatar :size="80" :src="volunteerDetail.volunteer.avatar || defaultAvatar" />
          <div class="volunteer-info">
            <h3 class="volunteer-name">{{ volunteerDetail.volunteer.realName }}</h3>
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
            {{ volunteerDetail.volunteer.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ volunteerDetail.volunteer.gender === 1 ? '男' : volunteerDetail.volunteer.gender === 2 ? '女' : '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ volunteerDetail.volunteer.age || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="地址">
            {{ volunteerDetail.volunteer.address || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="服务总时长">
            {{ volunteerDetail.volunteer.totalServiceHours || 0 }} 小时
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="reviews-section">
          <h4 class="section-title">
            <el-icon><ChatDotRound /></el-icon>
            用户评价（{{ volunteerDetail.reviews?.length || 0 }}条）
          </h4>
          <el-empty v-if="!volunteerDetail.reviews || volunteerDetail.reviews.length === 0" description="暂无评价" />
          <div v-else class="reviews-list">
            <div v-for="review in volunteerDetail.reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <div class="review-user">
                  <el-avatar :size="40" />
                  <div class="review-user-info">
                    <div class="review-username">{{ review.userName || '匿名用户' }}</div>
                    <el-rate v-model="review.rating" disabled show-score size="small" />
                  </div>
                </div>
                <div class="review-time">{{ formatTime(review.createTime) }}</div>
              </div>
              <div class="review-content" v-if="review.comment">
                {{ review.comment }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button size="large" @click="volunteerDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, List, Location, CircleCheck, CloseBold, Back, Star, Edit, User, ChatDotRound
} from '@element-plus/icons-vue'
import { getUserOrderDetail, cancelOrder, confirmOrder } from '@/api/order'
import { getVolunteerDetail } from '@/api/user'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

onMounted(() => {
  window.addEventListener(`orderStatusChange_${orderId}`, handleOrderStatusChange)
})

onUnmounted(() => {
  window.removeEventListener(`orderStatusChange_${orderId}`, handleOrderStatusChange)
})

const handleOrderStatusChange = (event: CustomEvent) => {
  const data = event.detail
  ElMessage.success(`订单状态已更新: ${getStatusText(data.newStatus)}`)

  setTimeout(() => {
    loadOrderDetail()
  }, 500)
}

const loading = ref(true)
const cancelLoading = ref(false)
const confirmLoading = ref(false)
const order = ref<any>(null)

const serviceDetailVisible = ref(false)
const currentService = ref<any>(null)

const volunteerDetailVisible = ref(false)
const volunteerDetail = ref<any>(null)
const defaultAvatar = ref('/src/assets/佐仓杏子.png')

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
    5: 'danger'
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
    5: '已取消'
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

const loadOrderDetail = async () => {
  loading.value = true
  try {
    const res = await getUserOrderDetail(orderId)
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

const showVolunteerDetail = async (volunteerId: number) => {
  serviceDetailVisible.value = false

  try {
    const res = await getVolunteerDetail(volunteerId)
    if (res.code === 200) {
      console.log('志愿者详情数据:', res.data)
      volunteerDetail.value = res.data
      volunteerDetailVisible.value = true
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取志愿者信息失败')
  }
}

const handleCancelOrder = async () => {
  if (!order.value?.id) return

  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '不取消',
      type: 'warning'
    })

    cancelLoading.value = true
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await loadOrderDetail(order.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消订单失败')
    }
  } finally {
    cancelLoading.value = false
  }
}

const handleConfirmOrder = async () => {
  if (!order.value?.id) return

  try {
    await ElMessageBox.confirm('确认服务已完成？', '确认完成', {
      confirmButtonText: '确认完成',
      cancelButtonText: '取消',
      type: 'success'
    })

    confirmLoading.value = true
    await confirmOrder(order.value.id)
    ElMessage.success('已确认服务完成')
    await loadOrderDetail(order.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '确认完成失败')
    }
  } finally {
    confirmLoading.value = false
  }
}

const goToReview = () => {
  if (!order.value?.id) return
  router.push(`/user/order/review/${order.value.id}`)
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
  flex-wrap: wrap;
}

.service-name-link {
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.service-name-link:hover {
  opacity: 0.8;
}

.no-volunteer-tip {
  margin-left: 8px;
}

.volunteer-link {
  font-size: 12px;
  margin-left: 8px;
}

.volunteer-link .el-icon {
  margin-right: 4px;
}

.volunteer-detail {
  padding: 0 10px;
}

.volunteer-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
}

.volunteer-info {
  flex: 1;
}

.volunteer-name {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0 0 12px 0;
}

.volunteer-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.reviews-section {
  margin-top: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.review-item:hover {
  background: #e8edf3;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-username {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.review-time {
  font-size: 12px;
  color: #999;
}

.review-content {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
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
</style>
