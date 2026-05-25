<template>
  <div class="todo-container">
    <h2 class="page-title">我的待办任务</h2>
    <div class="order-list">
      <div class="order-card" v-for="order in todoList" :key="order.id">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.id }}</span>
          <el-tag :type="getTagType(order.status)" size="large">
            {{ order.status }}
          </el-tag>
        </div>
        <div class="order-body">
          <div class="order-info">
            <div class="service-type">{{ order.serviceType }}</div>
            <div class="service-time">服务时间：{{ order.serviceTime }}</div>
            <div class="address">服务地址：{{ order.address }}</div>
          </div>
        </div>
        <div class="order-footer">
          <el-button type="primary" size="large" @click="confirmTask(order)">
            确认接单
          </el-button>
        </div>
      </div>
    </div>
    <div class="empty-tip" v-if="todoList.length === 0">暂无待办任务</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const getTagType = (status: string) => {
  if (status === '待确认') return 'warning'
  if (status === '服务中') return 'primary'
  return 'info'
}

const todoList = ref<any[]>([
  { id: 'ORD20260520001', serviceType: '助餐服务-营养套餐A', status: '待确认', serviceTime: '2026-05-21 11:30', address: '西安市雁塔区科技二路66号' },
  { id: 'ORD20260520002', serviceType: '助洁服务-日常保洁', status: '服务中', serviceTime: '2026-05-21 14:00', address: '西安市高新区锦业路1号' }
])

const confirmTask = (order: any) => {
  ElMessage.success(`已确认接单：${order.id}`)
}
</script>

<style scoped>
.todo-container {
  max-width: 900px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
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
.order-info .service-type { font-size: 20px; font-weight: 600; color: #006d5c; margin-bottom: 8px; }
.order-info .service-time, .order-info .address { font-size: 16px; color: #999; margin-bottom: 4px; }
.order-footer { text-align: right; margin-top: 16px; }
.empty-tip { text-align: center; font-size: 20px; color: #999; padding: 60px 0; }
</style>
