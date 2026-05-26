<template>
  <div class="page-container">
    <h2 class="page-title">差评管理</h2>
    <el-table :data="feedbackList" border>
      <el-table-column prop="orderId" label="订单号" />
      <el-table-column prop="user" label="评价用户" />
      <el-table-column prop="content" label="差评内容" />
      <el-table-column prop="time" label="时间" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const feedbackList = ref([
  { orderId: 'ORD001', user: '李奶奶', content: '送餐延迟了1小时', time: '2026-05-24' }
])
</script>

<style scoped>
.page-container { max-width: 1200px; }
.page-title { font-size: 28px; font-weight: bold; color: #333; margin-bottom: 24px; }
</style>
