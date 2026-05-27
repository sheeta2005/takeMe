<template>
  <div class="order-page">
    <h2 class="page-title">我的订单</h2>

    <!-- 订单状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" size="large">
        <el-radio-button :label="''">全部</el-radio-button>
        <el-radio-button :label="0">待接单</el-radio-button>
        <el-radio-button :label="1">已接单</el-radio-button>
        <el-radio-button :label="2">服务中</el-radio-button>
        <el-radio-button :label="3">待确认</el-radio-button>
        <el-radio-button :label="4">已完成</el-radio-button>
        <el-radio-button :label="5">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <div class="order-card" v-for="order in filteredOrderList" :key="order.id">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.id }}</span>
          <el-tag :type="getTagType(order.status)" size="large">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
        <div class="order-body">
          <div class="order-info">
            <div class="service-type">{{ order.serviceType }}</div>
            <div class="create-time">下单时间：{{ order.createTime }}</div>
          </div>
          <div class="order-price">¥{{ order.price }}</div>
        </div>
        <div class="order-footer">
          <!-- ✅ 新增：一键联系志愿者（仅已接单/服务中显示） -->
          <el-button
            v-if="[1, 2].includes(order.status) && order.volunteerPhone"
            type="success"
            size="large"
            @click="callVolunteer(order.volunteerPhone)"
          >
            联系志愿者
          </el-button>
          <el-button type="primary" size="large" @click="viewDetail(order)">
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-tip" v-if="filteredOrderList.length === 0">
      暂无订单记录
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

// 状态码映射
const statusMap = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '待确认',
  4: '已完成',
  5: '已取消'
}

// 状态标签颜色映射
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

// 筛选状态
const filterStatus = ref('')

// 订单列表（新增volunteerPhone字段）
const orderList = ref<any[]>([])

// 筛选后的订单
const filteredOrderList = computed(() => {
  if (filterStatus.value === '') return orderList.value
  return orderList.value.filter(item => item.status === filterStatus.value)
})

// 模拟数据（新增volunteerPhone字段）
const fetchOrders = async () => {
  orderList.value = [
    { id: 'ORD20260520001', serviceType: '助餐服务-营养套餐A', status: 0, createTime: '2026-05-20 10:30:00', price: 15, volunteerPhone: '' },
    { id: 'ORD20260519001', serviceType: '助洁服务-日常保洁', status: 2, createTime: '2026-05-19 14:00:00', price: 30, volunteerPhone: '13800138000' },
    { id: 'ORD20260518001', serviceType: '代购服务-生活用品代购', status: 3, createTime: '2026-05-18 09:15:00', price: 10, volunteerPhone: '13900139000' },
    { id: 'ORD20260517001', serviceType: '助医服务-陪同就诊', status: 4, createTime: '2026-05-17 08:00:00', price: 25, volunteerPhone: '13700137000' }
  ]
}

// ✅ 一键拨打志愿者电话
const callVolunteer = (phone: string) => {
  // 移动端会直接跳转拨号，PC端会提示复制号码
  window.location.href = `tel:${phone}`
  ElMessage.success('正在拨打志愿者电话...')
}

// 查看详情
const viewDetail = (order: any) => {
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
}
.filter-bar {
  margin-bottom: 32px;
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
:deep(.el-button) {
  font-size: 18px;
  padding: 12px 32px;
}
</style>
