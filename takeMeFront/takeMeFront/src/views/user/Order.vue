<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的订单</h2>
      <p class="page-subtitle">查看您的所有订单记录</p>
    </div>

    <div class="filter-bar" v-if="!loading">
      <el-input
        v-model="searchOrderNo"
        placeholder="搜索订单号"
        clearable
        size="large"
        style="width: 250px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
        v-model="filterStatus"
        placeholder="筛选订单状态"
        clearable
        size="large"
        style="width: 200px; margin-left: 12px"
        @change="handleFilterChange"
      >
        <el-option label="全部状态" :value="undefined" />
        <el-option label="未支付" :value="6" />
        <el-option label="待接单" :value="0" />
        <el-option label="已接单" :value="1" />
        <el-option label="服务中" :value="2" />
        <el-option label="待确认" :value="3" />
        <el-option label="已完成" :value="4" />
        <el-option label="已取消" :value="5" />
      </el-select>

      <el-button type="primary" size="large" @click="handleSearch" style="margin-left: 12px">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <el-skeleton :rows="5" animated v-if="loading" />

    <template v-else>
      <el-empty v-if="orderList.length === 0" description="暂无订单记录" :image-size="200" />

      <div v-else class="order-list">
        <el-card
          v-for="order in orderList"
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
            <el-tag :type="getStatusTagType(order.status)" size="large" effect="dark">
              {{ getStatusText(order.status) }}
            </el-tag>
          </div>

          <el-divider />

          <div class="order-card-body">
            <div class="info-row">
              <el-icon class="info-icon"><Calendar /></el-icon>
              <span class="info-text">下单时间：{{ formatTime(order.createTime) }}</span>
            </div>
            <div class="info-row">
              <el-icon class="info-icon"><Location /></el-icon>
              <span class="info-text">服务地址：{{ order.address }}</span>
            </div>
            <div class="info-row" v-if="order.items && order.items.length > 0">
              <el-icon class="info-icon"><List /></el-icon>
              <span class="info-text">服务项目：</span>
              <div class="service-tags">
                <el-tag
                  v-for="item in order.items"
                  :key="item.id"
                  size="small"
                  :type="getServiceTypeTag(item.serviceType)"
                  class="service-tag-item"
                >
                  {{ item.serviceName }} ×{{ item.quantity }}
                  <span v-if="item.volunteerId" class="volunteer-indicator">
                    👤 已接取
                  </span>
                </el-tag>
              </div>
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
            <div class="footer-actions">
              <el-button
                v-if="order.status === 6"
                type="success"
                size="small"
                @click.stop="handleRepay(order.id)"
              >
                去支付
              </el-button>
              <div class="action-hint">
                <el-icon><Right /></el-icon>
                查看详情
              </div>
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
import { Calendar, Location, Document, Right, List, Search } from '@element-plus/icons-vue'
import { getMyOrderList } from '@/api/order'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(true)
const orderList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref<number | undefined>(undefined)
const searchOrderNo = ref('')

const statusMap: Record<number, string> = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '待确认',
  4: '已完成',
  5: '已取消',
  6: '未支付'
}

const statusTagMap: Record<number, string> = {
  0: 'warning',
  1: 'primary',
  2: 'primary',
  3: 'info',
  4: 'success',
  5: 'danger',
  6: 'info'
}

const serviceTypeTagMap: Record<number, string> = {
  0: 'primary',
  1: 'success',
  2: 'warning',
  3: 'danger',
  4: 'info'
}

const getStatusText = (status: number) => {
  return statusMap[status] || '未知状态'
}

const getStatusTagType = (status: number) => {
  return statusTagMap[status] || 'info'
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

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getMyOrderList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value,
      orderNo: searchOrderNo.value || undefined
    })
    if (res.code === 200) {
      orderList.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载订单失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadOrders()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadOrders()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadOrders()
}

const goToDetail = (orderId: number) => {
  if (!orderId) {
    ElMessage.error('订单ID无效')
    return
  }
  router.push(`/user/order/detail/${orderId}`)
}

const handleRepay = (orderId: number) => {
  router.push(`/user/payment?orderId=${orderId}`)
}

onMounted(() => {
  loadOrders()
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

.filter-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
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

.service-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.service-tag-item {
  margin-right: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.volunteer-indicator {
  font-size: 12px;
  margin-left: 4px;
  opacity: 0.8;
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

.footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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
