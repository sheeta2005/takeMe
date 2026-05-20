<template>
  <div class="order-page">
    <h2 class="page-title">我的订单</h2>

    <!-- 订单状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" size="large">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="待接单">待接单</el-radio-button>
        <el-radio-button label="服务中">服务中</el-radio-button>
        <el-radio-button label="已完成">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <div class="order-card" v-for="order in filteredOrderList" :key="order.id">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.id }}</span>
          <el-tag :type="getTagType(order.status)" size="large">
            {{ order.status }}
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
import { getMyOrderList, getOrderDetail, cancelOrder } from '@/api/order'

const router = useRouter()

// 订单状态与标签颜色映射
const getTagType = (status: string) => {
  if (status === '待接单') return 'warning'
  if (status === '服务中') return 'primary'
  if (status === '已完成') return 'success'
  return 'info'
}

// 筛选状态
const filterStatus = ref('')

// 订单列表
const orderList = ref<any[]>([])

// 筛选后的订单
const filteredOrderList = computed(() => {
  if (!filterStatus.value) return orderList.value
  return orderList.value.filter(item => item.status === filterStatus.value)
})

// ==============================================
// 🔥 目前使用：模拟订单数据
// ==============================================
const fetchOrders = async () => {
  orderList.value = [
    { id: 'ORD20260520001', serviceType: '助餐服务-营养套餐A', status: '待接单', createTime: '2026-05-20 10:30:00', price: 15 },
    { id: 'ORD20260519001', serviceType: '助洁服务-日常保洁', status: '服务中', createTime: '2026-05-19 14:00:00', price: 30 },
    { id: 'ORD20260518001', serviceType: '代购服务-生活用品代购', status: '已完成', createTime: '2026-05-18 09:15:00', price: 10 }
  ]
}

// ==============================================
// ✅ 真实API版（已写好，以后解开注释直接用）
// 需要导入：import { getMyOrderList } from '@/api/order'
// ==============================================
/*
const fetchOrders = async () => {
  try {
    const res = await getMyOrderList()
    orderList.value = res.data
  } catch (err) {
    ElMessage.error('获取订单失败')
  }
}
*/

// 查看详情
const viewDetail = (order: any) => {
  router.push({ path: '/user/order/detail', query: { id: order.id } })
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
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.filter-bar {
  margin-bottom: 24px;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.order-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.order-id { font-size: 18px; color: #666; }
.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.service-type { font-size: 20px; font-weight: 600; color: #006d5c; }
.create-time { font-size: 16px; color: #999; margin-top: 8px; }
.order-price { font-size: 22px; color: #f56c6c; font-weight: bold; }
.order-footer { text-align: right; }
.empty-tip {
  text-align: center;
  font-size: 20px;
  color: #999;
  padding: 60px 0;
}
</style>
