<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">接取服务</h2>
      <p class="page-subtitle">浏览并接取可提供的服务项目</p>
    </div>

    <el-skeleton :rows="3" animated v-if="loading" />

    <template v-else>
      <el-alert
        v-if="hasInProgressService"
        title="您有正在进行中的服务，请先完成当前服务后再接取新服务"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 24px"
      />

      <el-empty v-if="availableServices.length === 0 && !hasInProgressService" description="暂无可接取的服务" :image-size="200">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </el-empty>

      <template v-else-if="!hasInProgressService">
        <div class="service-grid">
          <el-card
            v-for="service in availableServices"
            :key="service.id"
            class="service-card"
            shadow="hover"
          >
            <div class="service-card-header">
              <div class="order-info">
                <span class="order-label">订单编号</span>
                <span class="order-no">{{ service.orderNo }}</span>
              </div>
              <el-tag type="info" size="large">
                <el-icon><Bell /></el-icon>
                待接取
              </el-tag>
            </div>

            <el-divider />

            <div class="service-card-body">
              <div class="info-item">
                <el-icon class="info-icon"><Ticket /></el-icon>
                <span class="info-label">服务项目：</span>
                <div class="service-detail">
                  <el-tag :type="getServiceTypeTag(service.serviceType)" size="small">
                    {{ getServiceTypeName(service.serviceType) }}
                  </el-tag>
                  <span class="service-name">{{ service.serviceName }}</span>
                  <span class="service-quantity">×{{ service.quantity }}</span>
                </div>
              </div>

              <div class="info-item">
                <el-icon class="info-icon"><Calendar /></el-icon>
                <span class="info-label">服务时间：</span>
                <span class="info-value">{{ service.serviceDate }} {{ service.serviceTime }}</span>
              </div>

              <div class="info-item">
                <el-icon class="info-icon"><Location /></el-icon>
                <span class="info-label">服务地址：</span>
                <span class="info-value">{{ service.address }}</span>
              </div>

              <div class="info-item" v-if="service.remark">
                <el-icon class="info-icon"><Document /></el-icon>
                <span class="info-label">备注：</span>
                <span class="info-value">{{ service.remark }}</span>
              </div>
            </div>

            <div class="service-card-footer">
              <div class="price-info">
                <span class="price-label">服务金额：</span>
                <span class="price-value">¥{{ service.itemPrice }}</span>
              </div>
              <div class="action-buttons">
                <el-button type="primary" size="large" @click="acceptService(service)">
                  <el-icon><Check /></el-icon>
                  确认接取
                </el-button>
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
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Bell, Ticket, Calendar, Location, Document, Check } from '@element-plus/icons-vue'
import { getAvailableOrderList, confirmOrder, getVolunteerOrderList } from '@/api/volunteer'
import { useVolunteerStore } from '@/stores/volunteer'

const volunteerStore = useVolunteerStore()
const loading = ref(true)
const availableServices = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasInProgressService = ref(false)

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

const checkInProgressService = async () => {
  try {
    const res = await getVolunteerOrderList({
      pageNum: 1,
      pageSize: 1
    })
    if (res.code === 200) {
      let hasInProgress = false
      res.data?.records?.forEach((order: any) => {
        order.items?.forEach((item: any) => {
          if (item.volunteerId && item.itemStatus === 1) {
            hasInProgress = true
          }
        })
      })
      hasInProgressService.value = hasInProgress
    }
  } catch (error: any) {
    console.error('检查进行中服务失败:', error)
  }
}

const loadAvailableServices = async () => {
  loading.value = true
  try {
    const res = await getAvailableOrderList({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      const services: any[] = []
      res.data?.records?.forEach((order: any) => {
        order.items?.forEach((item: any) => {
          if (!item.volunteerId && item.itemStatus === 0) {
            services.push({
              ...item,
              orderNo: order.orderNo,
              orderId: order.id
            })
          }
        })
      })
      availableServices.value = services
      total.value = res.data?.total || 0
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载服务列表失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  currentPage.value = 1
  loadAvailableServices()
  checkInProgressService()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadAvailableServices()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadAvailableServices()
}

const acceptService = async (service: any) => {
  if (hasInProgressService.value) {
    ElMessage.warning('您有正在进行中的服务，请先完成当前服务后再接取新服务')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认接取服务 ${service.serviceName}？接取后将立即开始服务。`,
      '确认接取',
      {
        confirmButtonText: '确认接取',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await confirmOrder(service.id)
    ElMessage.success('接取成功')
    await checkInProgressService()
    await loadAvailableServices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '接取失败')
    }
  }
}

onMounted(async () => {
  await checkInProgressService()
  await loadAvailableServices()
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

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.service-card {
  border-radius: 12px;
  transition: all 0.3s ease;
}

.service-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 184, 153, 0.15);
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

.service-detail {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.service-name {
  font-weight: 500;
  flex: 1;
}

.service-quantity {
  color: #666;
  font-size: 14px;
}

.service-card-footer {
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

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
