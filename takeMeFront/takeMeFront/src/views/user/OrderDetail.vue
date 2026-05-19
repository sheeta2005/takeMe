<template>
  <div class="detail-page">
    <h2 class="page-title">订单详情</h2>

    <div class="detail-card" v-if="order">
      <div class="status-bar">
        <span class="order-id">订单号：{{ order.id }}</span>
        <el-tag :type="getTagType(order.status)" size="large">
          {{ order.status }}
        </el-tag>
      </div>

      <div class="info-section">
        <div class="info-item">
          <span class="label">服务类型：</span>
          <span class="value">{{ order.serviceType }}</span>
        </div>
        <div class="info-item">
          <span class="label">下单时间：</span>
          <span class="value">{{ order.createTime }}</span>
        </div>
        <div class="info-item" v-if="order.status === '服务中'">
          <span class="label">开始时间：</span>
          <span class="value">{{ order.startTime }}</span>
        </div>
        <div class="info-item" v-if="order.status === '已完成'">
          <span class="label">结束时间：</span>
          <span class="value">{{ order.endTime }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务价格：</span>
          <span class="price">¥{{ order.price }}</span>
        </div>
        <div class="info-item">
          <span class="label">备注信息：</span>
          <span class="value">{{ order.remark || '无' }}</span>
        </div>
      </div>

      <div class="action-section">
        <el-button
          type="danger"
          size="large"
          v-if="order.status === '待接单'"
        >
          取消订单
        </el-button>
      </div>
    </div>

    <!-- 返回按钮：去掉箭头图标，文字完全居中 -->
    <el-button
      type="danger"
      size="large"
      class="back-btn"
      @click="$router.back()"
    >
      返回
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()

const getTagType = (status: string) => {
  if (status === '待接单') return 'warning'
  if (status === '服务中') return 'primary'
  if (status === '已完成') return 'success'
  return 'info'
}

const mockOrderData: any = {
  ORD20260520001: {
    id: 'ORD20260520001',
    serviceType: '助餐服务-营养套餐A',
    status: '待接单',
    createTime: '2026-05-20 10:30:00',
    price: 15,
    remark: '不要香菜，少盐'
  },
  ORD20260519001: {
    id: 'ORD20260519001',
    serviceType: '助洁服务-日常保洁',
    status: '服务中',
    createTime: '2026-05-19 14:00:00',
    startTime: '2026-05-19 14:10:00',
    price: 30,
    remark: ''
  },
  ORD20260518001: {
    id: 'ORD20260518001',
    serviceType: '代购服务-生活用品代购',
    status: '已完成',
    createTime: '2026-05-18 09:15:00',
    startTime: '2026-05-18 09:30:00',
    endTime: '2026-05-18 10:10:00',
    price: 10,
    remark: '请帮忙买一瓶酱油和纸巾'
  }
}

const orderId = computed(() => route.query.id as string)
const order = ref<any>(null)

const loadOrderDetail = () => {
  if (orderId.value && mockOrderData[orderId.value]) {
    order.value = mockOrderData[orderId.value]
  } else {
    ElMessage.error('订单不存在')
  }
}

onMounted(() => {
  loadOrderDetail()
})
</script>

<style scoped>
.detail-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 0 100px 0;
  position: relative;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 30px 0;
  text-align: center;
}

.detail-card {
  background: #fff;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.order-id {
  font-size: 18px;
  color: #666;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  align-items: center;
}

.label {
  font-size: 18px;
  color: #666;
  width: 120px;
}

.value {
  font-size: 18px;
  color: #333;
}

.price {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}

.action-section {
  text-align: center;
}

/* 返回按钮：文字完全居中 */
.back-btn {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 20px !important;
  padding: 14px 40px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}
</style>
