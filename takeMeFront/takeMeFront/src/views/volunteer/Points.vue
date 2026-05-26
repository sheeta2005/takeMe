<template>
  <div class="points-container">
    <h2 class="page-title">积分薪酬</h2>

    <div class="summary-card">
      <div class="summary-item">
        <span class="label">当前积分</span>
        <span class="value">{{ currentPoints }}</span>
      </div>
      <div class="summary-item">
        <span class="label">累计服务时长</span>
        <span class="value">{{ totalHours }} 小时</span>
      </div>
    </div>

    <div class="record-card">
      <h3 class="card-title">积分明细</h3>
      <div class="points-list">
        <div
          class="points-item"
          v-for="item in pointsList"
          :key="item.id"
          @click="goToOrderDetail(item.orderId)"
        >
          <span class="type">{{ item.type }}</span>
          <span class="points">+{{ item.points }} 积分</span>
          <span class="time">{{ item.time }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const currentPoints = ref(1250)
const totalHours = ref(42)
const pointsList = ref([
  { id: 1, orderId: 'ORD20260518001', type: '助餐服务', points: 50, time: '2026-05-18' },
  { id: 2, orderId: 'ORD20260517001', type: '助洁服务', points: 80, time: '2026-05-17' }
])

// ✅ 正确跳转（完全匹配你的路由）
const goToOrderDetail = (orderId) => {
  router.push(`/volunteer/order/${orderId}`)
}
</script>

<style scoped>
.points-container {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.summary-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  margin-bottom: 24px;
  display: flex;
  justify-content: space-around;
}
.summary-item .label { font-size: 18px; color: #666; }
.summary-item .value { font-size: 24px; font-weight: bold; color: #006d5c; margin-top: 8px; }
.record-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.card-title { font-size: 22px; color: #333; margin-bottom: 16px; }
.points-list { display: flex; flex-direction: column; gap: 12px; }
.points-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}
.points-item:hover {
  background-color: #f7f7f7;
}
.points-item:last-child { border-bottom: none; }
.type { font-size: 16px; color: #333; }
.points { font-size: 16px; color: #00a88d; font-weight: 600; }
.time { font-size: 14px; color: #999; }
</style>
