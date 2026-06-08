<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的待办</h2>
      <p class="page-subtitle">查看和管理您已接取的服务</p>
    </div>

    <el-skeleton :rows="3" animated v-if="loading" />

    <template v-else>
      <el-empty v-if="myServices.length === 0" description="暂无待办服务" :image-size="200">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </el-empty>

      <template v-else>
        <div class="service-list">
          <el-card
            v-for="service in myServices"
            :key="service.id"
            class="service-card"
            shadow="hover"
          >
            <div class="service-card-header">
              <div class="order-info">
                <span class="order-label">订单编号</span>
                <span class="order-no">{{ service.orderNo }}</span>
              </div>
              <el-tag :type="getStatusType(service.itemStatus)" size="large" effect="dark">
                {{ getStatusText(service.itemStatus) }}
              </el-tag>
            </div>

            <el-divider />

            <div class="service-card-body">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="服务项目">
                  <div class="service-item-info">
                    <el-tag :type="getServiceTypeTag(service.serviceType)">
                      {{ getServiceTypeName(service.serviceType) }}
                    </el-tag>
                    <span class="service-name">{{ service.serviceName }}</span>
                    <span class="service-detail">×{{ service.quantity }}</span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="服务时间">
                  <div class="time-info">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ service.serviceDate }} {{ service.serviceTime }}</span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="服务地址">
                  <div class="address-info">
                    <el-icon><Location /></el-icon>
                    <span>{{ service.address }}</span>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="备注" v-if="service.remark">
                  <el-alert :title="service.remark" type="info" :closable="false" show-icon />
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="service-card-footer">
              <div class="price-info">
                <span class="price-label">服务金额：</span>
                <span class="price-value">¥{{ service.itemPrice }}</span>
              </div>
              <div class="action-buttons">
                <el-button
                  v-if="service.itemStatus === 1"
                  type="success"
                  size="large"
                  @click="startService(service)"
                >
                  <el-icon><VideoPlay /></el-icon>
                  开始服务
                </el-button>
                <el-button
                  v-if="service.itemStatus === 2"
                  type="primary"
                  size="large"
                  @click="completeService(service)"
                >
                  <el-icon><CircleCheck /></el-icon>
                  完成服务
                </el-button>
                <el-button
                  v-if="service.itemStatus === 1 || service.itemStatus === 2"
                  type="danger"
                  size="large"
                  @click="abandonService(service)"
                >
                  <el-icon><CloseBold /></el-icon>
                  放弃服务
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, Calendar, Location, VideoPlay, CircleCheck, CloseBold
} from '@element-plus/icons-vue'
import {
  getVolunteerOrderList,
  completeOrder as apiCompleteOrder,
  abandonOrder as apiAbandonOrder,
  startService as apiStartService
} from '@/api/volunteer'

const router = useRouter()

const loading = ref(true)
const myServices = ref<any[]>([])

const statusMap: Record<number, string> = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '待确认',
  4: '已完成',
  5: '已放弃'
}

const statusTypeMap: Record<number, string> = {
  0: 'info',
  1: 'primary',
  2: 'warning',
  3: 'warning',
  4: 'success',
  5: 'info'
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
  return statusMap[status] || '未知'
}

const getStatusType = (status: number) => {
  return statusTypeMap[status] || 'info'
}

const getServiceTypeName = (type: number) => {
  return serviceTypeMap[type] || '未知'
}

const getServiceTypeTag = (type: number) => {
  return serviceTypeTagMap[type] || 'info'
}

const loadMyServices = async () => {
  loading.value = true
  try {
    const res = await getVolunteerOrderList({
      page: 1,
      pageSize: 50
    })
    if (res.code === 200) {
      const services: any[] = []
      res.data?.records?.forEach((order: any) => {
        order.items?.forEach((item: any) => {
          if (item.volunteerId && [1, 2, 3].includes(item.itemStatus)) {
            services.push({
              ...item,
              orderNo: order.orderNo,
              orderId: order.id
            })
          }
        })
      })
      myServices.value = services
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载服务列表失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadMyServices()
}

const startService = async (service: any) => {
  try {
    await ElMessageBox.confirm(
      `确认开始服务 ${service.serviceName}？`,
      '开始服务',
      {
        confirmButtonText: '确认开始',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await apiStartService(service.id)
    ElMessage.success('服务已开始')
    await loadMyServices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '开始服务失败')
    }
  }
}

const completeService = async (service: any) => {
  try {
    await ElMessageBox.confirm(
      `确认已完成服务 ${service.serviceName}？`,
      '完成服务',
      {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await apiCompleteOrder(service.id)
    ElMessage.success('服务已完成，等待用户确认')
    await loadMyServices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '完成服务失败')
    }
  }
}

const abandonService = async (service: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要放弃服务 ${service.serviceName} 吗？放弃后可由其他志愿者接取。`,
      '放弃服务',
      {
        confirmButtonText: '确定放弃',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await apiAbandonOrder(service.id)
    ElMessage.warning('服务已放弃')
    await loadMyServices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '放弃服务失败')
    }
  }
}

onMounted(() => {
  loadMyServices()
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

.service-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.service-card {
  border-radius: 12px;
  transition: all 0.3s ease;
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
  margin: 16px 0;
}

.service-item-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.service-name {
  font-weight: 500;
  flex: 1;
}

.service-detail {
  color: #666;
  font-size: 14px;
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
</style>
