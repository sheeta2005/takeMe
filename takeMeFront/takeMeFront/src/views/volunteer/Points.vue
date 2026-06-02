<template>
  <div class="points-container">
    <h2 class="page-title">积分薪酬</h2>

    <el-empty v-if="loading" description="加载中..." />

    <template v-else>
      <div class="summary-card">
        <div class="summary-item">
          <span class="label">当前积分</span>
          <span class="value">{{ summary.points || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="label">累计服务时长</span>
          <span class="value">{{ volunteerStore.totalServiceHours }} 小时</span>
        </div>
      </div>

      <div class="record-card">
        <h3 class="card-title">积分明细</h3>
        <div class="points-list">
          <el-empty v-if="pointsList.length === 0" description="暂无积分记录" />
          <div
            class="points-item"
            v-for="item in pointsList"
            :key="item.id"
            @click="goToOrderDetail(item.orderId)"
          >
            <span class="type">{{ item.description }}</span>
            <span class="points" :class="{ positive: item.points > 0, negative: item.points < 0 }">
              {{ item.points > 0 ? '+' : '' }}{{ item.points }} 积分
            </span>
            <span class="time">{{ item.createTime }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPointsSummary, getPointsList } from '@/api/volunteer'
import { useVolunteerStore } from '@/stores/volunteer'

const router = useRouter()
const volunteerStore = useVolunteerStore()

const loading = ref(false)
const summary = ref<any>({})
const pointsList = ref<any[]>([])

const loadPointsData = async () => {
  loading.value = true
  try {
    const [summaryRes, listRes] = await Promise.all([
      getPointsSummary(),
      getPointsList()
    ])

    if (summaryRes.code === 200) {
      summary.value = summaryRes.data || {}
    }

    if (listRes.code === 200) {
      pointsList.value = listRes.data || []
    }
  } catch {
    ElMessage.error('加载积分数据失败')
  } finally {
    loading.value = false
  }
}

const goToOrderDetail = (orderId?: number) => {
  if (orderId) {
    router.push(`/volunteer/order/${orderId}`)
  }
}

onMounted(() => {
  loadPointsData()
})
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
.points { font-size: 16px; font-weight: 600; }
.points.positive { color: #00a88d; }
.points.negative { color: #f56c6c; }
.time { font-size: 14px; color: #999; }
</style>
