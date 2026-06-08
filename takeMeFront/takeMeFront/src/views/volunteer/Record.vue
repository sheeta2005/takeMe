<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">服务记录</h2>
      <p class="page-subtitle">查看您已完成的服务项目</p>
    </div>

    <div class="filter-bar" v-if="!loading">
      <el-input
        v-model="searchOrderNo"
        placeholder="搜索订单号"
        clearable
        size="large"
        style="width: 300px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-button type="primary" size="large" @click="handleSearch" style="margin-left: 12px">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <el-skeleton :rows="5" animated v-if="loading" />

    <template v-else>
      <el-empty v-if="completedServices.length === 0" description="暂无服务记录" :image-size="200" />

      <div v-else class="service-list">
        <el-card
          v-for="service in completedServices"
          :key="service.id"
          class="service-card"
          shadow="hover"
          @click="goToDetail(service.orderId)"
        >
          <div class="service-card-header">
            <div class="order-info">
              <span class="order-label">订单编号</span>
              <span class="order-no">{{ service.orderNo }}</span>
            </div>
            <el-tag type="success" size="large" effect="dark">
              <el-icon><CircleCheck /></el-icon>
              {{ getStatusText(service.itemStatus) }}
            </el-tag>
          </div>

          <el-divider />

          <div class="service-card-body">
            <div class="info-row">
              <el-icon class="info-icon"><Ticket /></el-icon>
              <span class="info-text">服务项目：</span>
              <el-tag :type="getServiceTypeTag(service.serviceType)" size="small">
                {{ getServiceTypeName(service.serviceType) }}
              </el-tag>
              <span class="service-name">{{ service.serviceName }}</span>
              <span class="service-detail">×{{ service.quantity }}</span>
            </div>

            <div class="info-row">
              <el-icon class="info-icon"><Calendar /></el-icon>
              <span class="info-text">服务时间：{{ service.serviceDate }} {{ service.serviceTime }}</span>
            </div>

            <div class="info-row">
              <el-icon class="info-icon"><Location /></el-icon>
              <span class="info-text">服务地址：{{ service.address }}</span>
            </div>

            <div class="info-row" v-if="service.remark">
              <el-icon class="info-icon"><Document /></el-icon>
              <span class="info-text">备注：{{ service.remark }}</span>
            </div>
          </div>

          <div class="service-card-footer">
            <div class="price-info">
              <span class="price-label">服务金额：</span>
              <span class="price-value">¥{{ service.itemPrice }}</span>
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
import { CircleCheck, Calendar, Location, Document, Right, Ticket, Search } from '@element-plus/icons-vue'
import { getVolunteerOrderList } from '@/api/volunteer'

const router = useRouter()

const loading = ref(true)
const completedServices = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchOrderNo = ref('')

const statusMap: Record<number, string> = {
  0: '待接单',
  1: '进行中',
  2: '待确认',
  3: '已完成',
  4: '已放弃'
}

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

const getStatusText = (status: number) => {
  return statusMap[status] || '未知状态'
}

const getServiceTypeName = (type: number) => {
  return serviceTypeMap[type] || '未知'
}

const getServiceTypeTag = (type: number) => {
  return serviceTypeTagMap[type] || 'info'
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getVolunteerOrderList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: 3
    })
    if (res.code === 200) {
      const services: any[] = []
      res.data?.records?.forEach((order: any) => {
        if (searchOrderNo.value && !order.orderNo.includes(searchOrderNo.value)) {
          return
        }
        order.items?.forEach((item: any) => {
          if (item.itemStatus === 3) {
            services.push({
              ...item,
              orderNo: order.orderNo,
              orderId: order.id
            })
          }
        })
      })
      completedServices.value = services
      total.value = services.length
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载服务记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadRecords()
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

.filter-bar {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.service-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.service-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
}

.service-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 184, 153, 0.15);
}

.service-card-header {
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

.service-card-body {
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

.service-name {
  font-weight: 500;
  color: #333;
}

.service-detail {
  color: #666;
  font-size: 14px;
}

.service-card-footer {
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
