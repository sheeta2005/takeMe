
<template>
  <div class="dashboard-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="never">
      <div class="banner-content">
        <div class="banner-left">
          <el-avatar :size="64" :src=" defaultAvatar" class="admin-avatar">
            <el-icon :size="32"><UserFilled /></el-icon>
          </el-avatar>
          <div class="banner-text">
            <h2 class="banner-title">早安，{{ adminStore.realName }}！</h2>
            <p class="banner-subtitle">今天是 {{ currentDate }}，祝您工作愉快</p>
          </div>
        </div>
        <div class="banner-right">
          <el-statistic title="今日待处理" :value="pendingCount">
            <template #suffix>
              <el-icon class="stat-icon"><Bell /></el-icon>
            </template>
          </el-statistic>
        </div>
      </div>
    </el-card>

    <!-- 核心数据统计 -->
    <div class="stats-grid">
      <el-card class="stat-card stat-card-primary" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-primary">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总订单数</div>
            <el-count-up :end-val="dashboardData.totalOrders" class="stat-value" />
            <div class="stat-trend trend-up">
              <el-icon><CaretTop /></el-icon>
              <span>12% 较上周</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-success" shadow="hover" @click="goToActiveOrders">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-success">
            <el-icon :size="32"><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">服务中订单</div>
            <el-count-up :end-val="dashboardData.activeOrders" class="stat-value" />
            <div class="stat-trend trend-stable">
              <el-icon><Minus /></el-icon>
              <span>稳定</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-warning" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-warning">
            <el-icon :size="32"><Money /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日营业额</div>
            <div class="stat-value currency">¥{{ dashboardData.todayRevenue }}</div>
            <div class="stat-trend trend-up">
              <el-icon><CaretTop /></el-icon>
              <span>8% 较昨日</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-info" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-info">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">志愿者人数</div>
            <el-count-up :end-val="dashboardData.volunteerCount" class="stat-value" />
            <div class="stat-trend trend-up">
              <el-icon><Plus /></el-icon>
              <span>+3 本月新增</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-danger" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-danger">
            <el-icon :size="32"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">老人用户数</div>
            <el-count-up :end-val="dashboardData.elderCount" class="stat-value" />
            <div class="stat-trend trend-up">
              <el-icon><Plus /></el-icon>
              <span>+15 本月新增</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-purple" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-purple">
            <el-icon :size="32"><Coin /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">本月积分发放</div>
            <el-count-up :end-val="dashboardData.pointsIssued" class="stat-value" />
            <div class="stat-trend trend-up">
              <el-icon><CaretTop /></el-icon>
              <span>25% 较上月</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="24" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><TrendCharts /></el-icon>
                <span class="card-title">订单趋势（近7天）</span>
              </div>
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="orderTrendChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><PieChart /></el-icon>
                <span class="card-title">服务类型分布</span>
              </div>
            </div>
          </template>
          <div ref="serviceTypeChart" class="chart-box pie-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 & 最近动态 -->
    <el-row :gutter="24" class="bottom-row">
      <el-col :xs="24" :md="12">
        <el-card class="quick-actions-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><Grid /></el-icon>
                <span class="card-title">快捷操作</span>
              </div>
            </div>
          </template>
          <div class="action-grid">
            <div class="action-item" @click="router.push('/admin/order')">
              <div class="action-icon bg-primary-light">
                <el-icon :size="28"><Document /></el-icon>
              </div>
              <div class="action-text">订单管理</div>
            </div>
            <div class="action-item" @click="router.push('/admin/volunteer')">
              <div class="action-icon bg-success-light">
                <el-icon :size="28"><User /></el-icon>
              </div>
              <div class="action-text">志愿者管理</div>
            </div>
            <div class="action-item" @click="router.push('/admin/user')">
              <div class="action-icon bg-warning-light">
                <el-icon :size="28"><UserFilled /></el-icon>
              </div>
              <div class="action-text">老人管理</div>
            </div>
            <div class="action-item" @click="router.push('/admin/approval')">
              <div class="action-icon bg-info-light">
                <el-icon :size="28"><Check /></el-icon>
              </div>
              <div class="action-text">业务审批</div>
            </div>
            <div class="action-item" @click="router.push('/admin/sendMsg')">
              <div class="action-icon bg-danger-light">
                <el-icon :size="28"><ChatDotRound /></el-icon>
              </div>
              <div class="action-text">发送消息</div>
            </div>
            <div class="action-item" @click="router.push('/admin/message')">
              <div class="action-icon bg-purple-light">
                <el-icon :size="28"><Message /></el-icon>
              </div>
              <div class="action-text">消息中心</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card class="recent-orders-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><Clock /></el-icon>
                <span class="card-title">最近订单</span>
              </div>
              <el-button type="primary" link @click="router.push('/admin/order')">
                查看全部
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <el-table :data="recentOrders" style="width: 100%" :show-header="false">
            <el-table-column prop="orderNo" label="订单号" width="180">
              <template #default="{ row }">
                <span class="order-no">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="120">
              <template #default="{ row }">
                <span class="amount">¥{{ row.totalAmount?.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间">
              <template #default="{ row }">
                <span class="time-text">{{ formatTime(row.createTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDashboardData, getOrderTrend7d, getServiceTypeDist, searchOrder } from '@/api/admin'
import { useAdminStore } from '@/stores/admin'
import defaultAvatar from '@/assets/default-avatar.png'
import {
  UserFilled, Bell, Document, Loading, Money, User, Coin,
  CaretTop, Minus, Plus, TrendCharts, PieChart, Grid, Check,
  ChatDotRound, Message, Clock, ArrowRight
} from '@element-plus/icons-vue'

const router = useRouter()
const adminStore = useAdminStore()

const pendingCount = ref(0)
const trendPeriod = ref('week')
const recentOrders = ref<any[]>([])

const currentDate = computed(() => {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${week[now.getDay()]}`
})

const dashboardData = ref({
  totalOrders: 0,
  activeOrders: 0,
  todayRevenue: 0,
  volunteerCount: 0,
  elderCount: 0,
  pointsIssued: 0
})

const orderTrendChart = ref<HTMLDivElement | null>(null)
const serviceTypeChart = ref<HTMLDivElement | null>(null)

const goToActiveOrders = () => {
  router.push({
    path: '/admin/order',
    query: { status: 'active' }
  })
}

const getStatusName = (status: number) => {
  const names = ['待分配', '服务中', '已完成', '已评价', '已取消', '已拒绝']
  return names[status] || '未知'
}

const getStatusType = (status: number) => {
  const types = ['warning', 'primary', 'success', 'success', 'info', 'danger']
  return types[status] || ''
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getRecent7Days = () => {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    days.push(`${date.getMonth() + 1}/${date.getDate()}`)
  }
  return days
}

const initCharts = async () => {
  if (orderTrendChart.value) {
    const chart = echarts.init(orderTrendChart.value)
    try {
      const res = await getOrderTrend7d()
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.95)',
          textStyle: { color: '#333' },
          borderRadius: 8,
          padding: [12, 16]
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: getRecent7Days(),
          axisLine: { lineStyle: { color: '#eee' } },
          axisLabel: { color: '#666' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f5f5f5' } },
          axisLabel: { color: '#666' }
        },
        series: [{
          name: '订单数',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          data: res.data,
          color: '#00a88d',
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0,168,141,0.3)' },
              { offset: 1, color: 'rgba(0,168,141,0.05)' }
            ])
          },
          lineStyle: { width: 3 },
          itemStyle: { borderWidth: 2, borderColor: '#fff' }
        }]
      })
    } catch (err) {
      console.error('获取订单趋势失败', err)
    }
  }

  if (serviceTypeChart.value) {
    const chart = echarts.init(serviceTypeChart.value)
    try {
      const res = await getServiceTypeDist()
      chart.setOption({
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderRadius: 8,
          padding: [12, 16]
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          textStyle: { color: '#666' },
          itemWidth: 12,
          itemHeight: 12
        },
        series: [{
          name: '服务类型',
          type: 'pie',
          radius: ['50%', '70%'],
          center: ['35%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 3
          },
          label: { show: false },
          emphasis: {
            label: {
              show: true,
              fontSize: 16,
              fontWeight: 'bold',
              color: '#333'
            },
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            }
          },
          data: res.data.map((item: any, index: number) => {
            const colors = ['#00a88d', '#36b9cc', '#1cc88a', '#4e73df', '#f6c23e']
            return {
              value: item.value,
              name: item.name,
              itemStyle: { color: colors[index % colors.length] }
            }
          })
        }]
      })
    } catch (err) {
      console.error('获取服务分布失败', err)
    }
  }
}

onMounted(async () => {
  try {
    const res = await getDashboardData()
    dashboardData.value = res.data
    pendingCount.value = res.data.activeOrders || 0

    const orderRes = await searchOrder(1, 5)
    if (orderRes.data) {
      recentOrders.value = orderRes.data.records || []
    }
  } catch (err) {
    console.error('获取工作台数据失败', err)
  }

  initCharts()
})

onUnmounted(() => {
  if (orderTrendChart.value) echarts.dispose(orderTrendChart.value)
  if (serviceTypeChart.value) echarts.dispose(serviceTypeChart.value)
})
</script>

<style scoped>
.dashboard-container {
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

.admin-avatar {
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

.stat-icon {
  font-size: 32px;
  color: #00a88d;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
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

.stat-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.bg-primary { background: linear-gradient(135deg, #00a88d 0%, #00c4a0 100%); }
.bg-success { background: linear-gradient(135deg, #10b981 0%, #34d399 100%); }
.bg-warning { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); }
.bg-info { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); }
.bg-danger { background: linear-gradient(135deg, #ef4444 0%, #f87171 100%); }
.bg-purple { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1e293b;
  margin-bottom: 4px;
}

.stat-value.currency {
  color: #f5222d;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.trend-up { color: #10b981; }
.trend-down { color: #ef4444; }
.trend-stable { color: #64748b; }

/* 图表区域 */
.chart-row {
  margin-bottom: 24px;
}

.chart-card {
  height: 100%;
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
  height: 320px;
}

.pie-chart {
  height: 280px;
}

/* 底部区域 */
.bottom-row {
  margin-bottom: 24px;
}

.quick-actions-card,
.recent-orders-card {
  height: 100%;
  border: 1px solid #e2e8f0;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-item:hover {
  background: #f8faf9;
  transform: translateY(-2px);
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-primary-light { background: rgba(0, 168, 141, 0.1); color: #00a88d; }
.bg-success-light { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.bg-warning-light { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.bg-info-light { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.bg-danger-light { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.bg-purple-light { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }

.action-text {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.order-no {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #3b82f6;
}

.amount {
  font-weight: 600;
  color: #f5222d;
}

.time-text {
  color: #64748b;
  font-size: 13px;
}
</style>
