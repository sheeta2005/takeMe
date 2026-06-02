<template>
  <div class="order-page">
    <h2 class="page-title">我的订单</h2>

    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" size="large" @change="fetchOrders">
        <el-radio-button :value="''">全部</el-radio-button>
        <el-radio-button :value="0">待接单</el-radio-button>
        <el-radio-button :value="1">已接单</el-radio-button>
        <el-radio-button :value="2">服务中</el-radio-button>
        <el-radio-button :value="3">待确认</el-radio-button>
        <el-radio-button :value="4">已完成</el-radio-button>
        <el-radio-button :value="5">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <div class="order-list" v-loading="loading">
      <div class="order-card" v-for="order in orderList" :key="order.id">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.orderNo }}</span>
          <el-tag :type="getTagType(order.status)" size="large">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
        <div class="order-body">
          <div class="order-info">
            <div class="service-type">
              {{ order.items?.[0]?.serviceName || '服务套餐' }}
              <span v-if="order.items && order.items.length > 1"> 等{{ order.items.length }}项服务</span>
            </div>
            <div class="create-time">下单时间：{{ formatTime(order.createTime) }}</div>
          </div>
          <div class="order-price">¥{{ order.totalPrice }}</div>
        </div>
        <div class="order-footer">
          <el-button type="primary" size="large" @click="viewDetail(order)">
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <div class="empty-tip" v-if="orderList.length === 0 && !loading">
      暂无订单记录
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchOrders"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getMyOrderList } from '@/api/order'

const router = useRouter()

const statusMap: Record<number, string> = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '待确认',
  4: '已完成',
  5: '已取消'
}

const getTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'info',
    4: 'success',
    5: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => statusMap[status] || '未知状态'

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

const filterStatus = ref<string | number>('')
const orderList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getMyOrderList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value ? Number(filterStatus.value) : undefined
    })

    if (res.code === 200 && res.data) {
      orderList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      orderList.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('获取订单失败:', e)
    ElMessage.error('获取订单失败')
    orderList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchOrders()
}

const viewDetail = (order: any) => {
  if (!order?.id) {
    ElMessage.error('订单信息异常！')
    return
  }
  router.push(`/user/order/detail/${order.id}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}
.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 32px;
  text-align: center;
}
.filter-bar {
  margin-bottom: 32px;
  display: flex;
  justify-content: center;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.order-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
  transition: all 0.3s;
}
.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 184, 153, 0.15);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.order-id { font-size: 20px; color: #666; }
.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.service-type { font-size: 24px; font-weight: 600; color: #006d5c; }
.create-time { font-size: 18px; color: #999; margin-top: 12px; }
.order-price { font-size: 26px; color: #f56c6c; font-weight: bold; }
.order-footer {
  text-align: right;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}
.empty-tip {
  text-align: center;
  font-size: 24px;
  color: #999;
  padding: 80px 0;
}
.pagination-wrapper {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}
:deep(.el-button) {
  font-size: 18px;
  padding: 12px 32px;
}
</style>
