
<template>
  <div class="home-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="hover">
      <div class="banner-content">
        <div class="banner-left">
          <el-avatar :size="72" :src="volunteerStore.avatar || defaultAvatar" class="volunteer-avatar">
            <el-icon :size="36"><UserFilled /></el-icon>
          </el-avatar>
          <div class="banner-text">
            <h2 class="banner-title">您好，{{ userName }} 志愿者 🤝</h2>
            <p class="banner-subtitle">感谢您加入助老服务平台，您的每一份付出，都是老人温暖的依靠</p>
          </div>
        </div>
        <div class="banner-stats">
          <el-statistic title="本月服务时长" :value="serviceHours" suffix="小时">
            <template #prefix>
              <el-icon color="#00a88d"><Clock /></el-icon>
            </template>
          </el-statistic>
        </div>
      </div>
    </el-card>

    <!-- 个人数据概览 -->
    <div class="stats-grid">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon bg-primary">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">累计服务订单</div>
            <div class="stat-value">{{ volunteerData.totalOrders }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon bg-success">
            <el-icon :size="28"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">已完成订单</div>
            <div class="stat-value">{{ volunteerData.completedOrders }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon bg-warning">
            <el-icon :size="28"><Coin /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">当前积分</div>
            <div class="stat-value">{{ volunteerData.points }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon bg-info">
            <el-icon :size="28"><Star /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">平均评分</div>
            <div class="stat-value">{{ volunteerData.rating }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 服务类型分布 & 待办提醒 -->
    <el-row :gutter="24">
      <el-col :xs="24" :lg="16">
        <el-card class="service-dist-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><PieChart /></el-icon>
                <span class="card-title">服务类型分布</span>
              </div>
            </div>
          </template>
          <div ref="serviceChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="todo-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#f59e0b"><Bell /></el-icon>
                <span class="card-title">待办提醒</span>
              </div>
              <el-badge :value="3" :max="99" />
            </div>
          </template>
          <div class="todo-list">
            <div class="todo-item" @click="router.push('/volunteer/todo')">
              <div class="todo-icon bg-warning-light">
                <el-icon :size="24"><Clock /></el-icon>
              </div>
              <div class="todo-info">
                <div class="todo-title">待接单任务</div>
                <div class="todo-desc">有3个订单等待您确认</div>
              </div>
              <el-icon :size="20" color="#94a3b8"><ArrowRight /></el-icon>
            </div>
            <div class="todo-item">
              <div class="todo-icon bg-info-light">
                <el-icon :size="24"><Calendar /></el-icon>
              </div>
              <div class="todo-info">
                <div class="todo-title">本周出勤</div>
                <div class="todo-desc">已出勤4天，剩余3天</div>
              </div>
              <el-icon :size="20" color="#94a3b8"><ArrowRight /></el-icon>
            </div>
            <div class="todo-item" @click="router.push('/volunteer/points')">
              <div class="todo-icon bg-success-light">
                <el-icon :size="24"><Money /></el-icon>
              </div>
              <div class="todo-info">
                <div class="todo-title">积分薪酬</div>
                <div class="todo-desc">本月可兑换 ¥200</div>
              </div>
              <el-icon :size="20" color="#94a3b8"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 平台公告 -->
    <el-card class="news-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#3b82f6"><InfoFilled /></el-icon>
            <span class="card-title">志愿者公告</span>
          </div>
        </div>
      </template>
      <div class="news-list">
        <div class="news-item" v-for="(item, i) in newsList" :key="i">
          <div class="news-left">
            <el-icon :size="20" color="#00a88d"><Document /></el-icon>
            <span class="news-title">{{ item.title }}</span>
          </div>
          <el-tag size="small" type="info">{{ item.date }}</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVolunteerStore } from '@/stores/volunteer'
import { getVolunteerOrderList, getPointsSummary } from '@/api/volunteer'
import * as echarts from 'echarts'
import {
  UserFilled, Clock, Document, CircleCheck, Coin, Star,
  PieChart, Bell, Calendar, Money, ArrowRight, InfoFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const volunteerStore = useVolunteerStore()
const userName = ref('')
const serviceHours = ref(0)
const serviceChart = ref<HTMLDivElement | null>(null)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const volunteerData = ref({
  totalOrders: 0,
  completedOrders: 0,
  points: 0,
  rating: '0.0'
})

onMounted(async () => {
  // 先获取志愿者最新信息（包括头像等）
  await volunteerStore.fetchVolunteerInfo()

  userName.value = volunteerStore.realName || '尊敬的志愿者'
  serviceHours.value = volunteerStore.totalServiceHours || 0

  try {
    const orderRes = await getVolunteerOrderList({ pageNum: 1, pageSize: 1 })
    if (orderRes.data) {
      volunteerData.value.totalOrders = orderRes.data.total || 0
    }
  } catch (e) {
    console.error('获取订单总数失败', e)
  }

  try {
    const completedRes = await getVolunteerOrderList({ pageNum: 1, pageSize: 1, status: 4 })
    if (completedRes.data) {
      volunteerData.value.completedOrders = completedRes.data.total || 0
    }
  } catch (e) {
    console.error('获取已完成订单数失败', e)
  }

  try {
    const pointsRes = await getPointsSummary()
    if (pointsRes.data) {
      volunteerData.value.points = pointsRes.data.totalPoints || 0
    }
  } catch (e) {
    console.error('获取积分失败', e)
  }

  initChart()
})

const newsList = ref([
  { title: '助餐服务：每日10点前接单，优先配送', date: '2025-01-01' },
  { title: '助洁服务：请按预约时间上门，提前10分钟联系老人', date: '2025-01-02' },
  { title: '助医服务：陪同就诊请提前熟悉医院流程，协助老人挂号', date: '2025-01-03' },
  { title: '代购服务：请核对药品/生活用品清单，确保无遗漏', date: '2025-01-04' },
])

const initChart = async () => {
  if (!serviceChart.value) return

  try {
    const res = await getVolunteerOrderList({ pageNum: 1, pageSize: 100 })
    const typeCounts: Record<number, number> = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 }
    if (res.data?.records) {
      res.data.records.forEach((order: any) => {
        order.items?.forEach((item: any) => {
          if (typeCounts[item.serviceType] !== undefined) {
            typeCounts[item.serviceType]++
          }
        })
      })
    }

    const chart = echarts.init(serviceChart.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center',
        textStyle: { color: '#64748b' }
      },
      series: [{
        name: '服务类型',
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: [
          { value: typeCounts[2], name: '助餐服务', itemStyle: { color: '#f59e0b' } },
          { value: typeCounts[1], name: '助洁服务', itemStyle: { color: '#3b82f6' } },
          { value: typeCounts[3], name: '助医服务', itemStyle: { color: '#10b981' } },
          { value: typeCounts[0], name: '代购服务', itemStyle: { color: '#8b5cf6' } },
          { value: typeCounts[4], name: '陪伴服务', itemStyle: { color: '#ef4444' } }
        ]
      }]
    })
  } catch (e) {
    console.error('加载图表数据失败', e)
  }
}
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 0;
}

/* 欢迎横幅 */
.welcome-banner {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f0fffc 100%);
  border: 1px solid rgba(0, 168, 141, 0.1);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.volunteer-avatar {
  border: 3px solid rgba(0, 168, 141, 0.2);
  box-shadow: 0 4px 12px rgba(0, 168, 141, 0.15);
}

.banner-title {
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.banner-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.bg-primary { background: linear-gradient(135deg, #00a88d 0%, #00c4a0 100%); }
.bg-success { background: linear-gradient(135deg, #10b981 0%, #34d399 100%); }
.bg-warning { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); }
.bg-info { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
}

/* 图表和待办 */
.service-dist-card,
.todo-card {
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  background: #f8faf9;
  cursor: pointer;
  transition: all 0.2s ease;
}

.todo-item:hover {
  background: #f0fffc;
}

.todo-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bg-warning-light { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.bg-info-light { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.bg-success-light { background: rgba(16, 185, 129, 0.1); color: #10b981; }

.todo-info {
  flex: 1;
}

.todo-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.todo-desc {
  font-size: 12px;
  color: #64748b;
}

/* 公告 */
.news-card {
  border: 1px solid #e2e8f0;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f8faf9;
  transition: all 0.2s ease;
}

.news-item:hover {
  background: #f0fffc;
}

.news-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.news-title {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .banner-content {
    flex-direction: column;
    text-align: center;
  }
}
</style>
