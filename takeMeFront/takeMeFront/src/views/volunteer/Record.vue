<template>
  <div class="record-container">
    <h2 class="page-title">服务记录</h2>
    <div class="record-list">
      <div class="record-card" v-for="record in recordList" :key="record.id">
        <div class="record-header">
          <span class="record-id">订单号：{{ record.id }}</span>
          <el-tag type="success" size="large">已完成</el-tag>
        </div>
        <div class="record-body">
          <div class="service-type">{{ record.serviceType }}</div>
          <div class="service-time">服务时间：{{ record.serviceTime }}</div>
          <div class="evaluation">老人评价：{{ record.evaluation }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const recordList = ref([
  { id: 'ORD20260518001', serviceType: '助餐服务-营养套餐A', serviceTime: '2026-05-18 11:30', evaluation: '服务态度很好，配送准时' },
  { id: 'ORD20260517001', serviceType: '助洁服务-日常保洁', serviceTime: '2026-05-17 14:00', evaluation: '打扫得很干净，很满意' }
])
</script>

<style scoped>
.record-container {
  max-width: 900px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.record-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.record-id { font-size: 18px; color: #666; }
.record-body .service-type { font-size: 20px; font-weight: 600; color: #006d5c; margin-bottom: 8px; }
.record-body .service-time, .record-body .evaluation { font-size: 16px; color: #999; margin-bottom: 4px; }
</style>
