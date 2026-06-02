<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">服务记录</h2>
      <p class="page-subtitle">查看您已完成的服务订单</p>
    </div>

    <el-skeleton :rows="5" animated v-if="loading" />

    <template v-else>
      <el-empty v-if="completedOrders.length === 0" description="暂无服务记录" :image-size="200" />

      <div v-else class="order-list">
        <el-card
          v-for="order in completedOrders"
          :key="order.id"
          class="order-card"
          shadow="hover"
          @click="goToDetail(order.id)"
        >
          <div class="order-card-header">
            <div class="order-info">
              <span class="order-label">订单编号</span>
              <span class="order-no">{{ order.orderNo }}</span>
            </div>
            <el-tag type="success" size="large" effect="dark">
              <el-icon><CircleCheck /></el-icon>
              {{ getStatusText(order.status) }}
            </el-tag>
          </div>

          <el-divider />

          <div class="order-card-body">
            <div class="info-row">
              <el-icon class="info-icon"><Calendar /></el-icon>
              <span class="info-text">服务时间：{{ order.serviceDate }} {{ order.serviceTime }}</span>
            </div>
            <div class="info-row">
              <el-icon class="info-icon"><Location /></el-icon>
              <span class="info-text">服务地址：{{ order.address }}</span>
            </div>
            <div class="info-row" v-if="order.remark">
              <el-icon class="info-icon"><Document /></el-icon>
              <span class="info-text">备注：{{ order.remark }}</span>
            </div>
          </div>

          <div class="order-card-footer">
            <div class="price-info">
              <span class="price-label">订单金额：</span>
              <span class="price-value">¥{{ order.totalPrice }}</span>
            </div>
            <div class="action-hint">
              <el-icon><Right /></el-icon>
              查看详情
            </div>
          </div>
        </el-card>
      </div>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          background
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Calendar, Location, Document, Right } from '@element-plus/icons-vue'
import { getVolunteerOrderList } from '@/api/volunteer'

const router = useRouter()

const loading = ref(true)
const completedOrders = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap: Record<number, string> = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '待确认',
  4: '已完成',
  5: '已取消',
  6: '已放弃'
}

const getStatusText = (status: number) => {
  return statusMap[status] || '未知状态'
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getVolunteerOrderList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: 4
    })
    if (res.code === 200) {
      completedOrders.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载服务记录失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadRecords()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadRecords()
}

const goToDetail = (orderId: number) => {
  router.push(`/volunteer/order/${orderId}`)
}

onMounted(() => {
  loadRecords()
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

.order-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.order-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 184, 153, 0.15);
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

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-icon {
  color: #00b899;
  font-size: 18px;
}

.info-text {
  font-size: 15px;
  color: #666;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
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

.action-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #00b899;
  font-size: 14px;
  font-weight: 500;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
