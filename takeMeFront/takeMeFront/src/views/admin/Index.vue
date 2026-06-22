
<template>
  <div class="dashboard-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="never">
      <div class="banner-content">
        <div class="banner-left">
          <el-avatar :size="64" :src="adminStore.avatar || defaultAvatar" class="admin-avatar">
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

    <!-- 核心数据统计 - 8个指标卡片 -->
    <div class="stats-grid">
      <!-- 用户板块 -->
      <el-card class="stat-card stat-card-user" shadow="hover" @click="router.push('/admin/user')">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-user">
            <el-icon :size="32"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总注册老人</div>
            <div class="stat-value">{{ dashboardData.elderCount }}</div>
            <div class="stat-trend trend-stable">
              <span>用户总数</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-user-light" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-user-light">
            <el-icon :size="32"><Connection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">在线老人</div>
            <div class="stat-value">{{ onlineStats.elderOnline || 0 }}</div>
            <div class="stat-trend trend-up">
              <el-icon><CaretTop /></el-icon>
              <span>实时在线</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-volunteer" shadow="hover" @click="router.push('/admin/volunteer')">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-volunteer">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">志愿者总数</div>
            <div class="stat-value">{{ dashboardData.volunteerCount }}</div>
            <div class="stat-trend trend-stable">
              <span>注册志愿者</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-volunteer-light" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-volunteer-light">
            <el-icon :size="32"><Connection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">在线志愿者</div>
            <div class="stat-value">{{ onlineStats.volunteerOnline || 0 }}</div>
            <div class="stat-trend trend-up">
              <el-icon><CaretTop /></el-icon>
              <span>实时在线</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 订单板块 -->
      <el-card class="stat-card stat-card-order" shadow="hover" @click="router.push('/admin/order')">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-order">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日下单量</div>
            <div class="stat-value">{{ dashboardData.todayOrders || 0 }}</div>
            <div class="stat-trend trend-stable">
              <span>今日新增</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-order-light" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-order-light">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">待处理订单</div>
            <div class="stat-value">{{ dashboardData.pendingOrders || 0 }}</div>
            <div class="stat-trend trend-warning">
              <el-icon><Warning /></el-icon>
              <span>需处理</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-revenue" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-revenue">
            <el-icon :size="32"><Money /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">本月成交总额</div>
            <div class="stat-value currency">¥{{ dashboardData.monthRevenue || 0 }}</div>
            <div class="stat-trend trend-up">
              <el-icon><CaretTop /></el-icon>
              <span>本月累计</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card stat-card-completed" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon-wrapper bg-completed">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">已完成订单</div>
            <div class="stat-value">{{ dashboardData.completedOrders || 0 }}</div>
            <div class="stat-trend trend-success">
              <el-icon><SuccessFilled /></el-icon>
              <span>累计完成</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 实时在线人员面板 -->
    <el-card class="online-panel-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Monitor /></el-icon>
            <span class="card-title">实时在线人员</span>
          </div>
          <el-tag type="success" effect="plain">
            <el-icon><Refresh /></el-icon>
            实时更新
          </el-tag>
        </div>
      </template>
      <div class="online-stats">
        <div class="online-item">
          <div class="online-icon bg-admin">
            <el-icon :size="24"><UserFilled /></el-icon>
          </div>
          <div class="online-info">
            <div class="online-label">管理员在线</div>
            <div class="online-value">{{ onlineStats.adminOnline || 0 }} 人</div>
          </div>
          <div class="online-chart">
            <div class="mini-bar" :style="{ height: Math.min((onlineStats.adminOnline || 0) * 10, 100) + 'px' }"></div>
          </div>
        </div>
        <div class="online-item">
          <div class="online-icon bg-volunteer-online">
            <el-icon :size="24"><User /></el-icon>
          </div>
          <div class="online-info">
            <div class="online-label">志愿者在线</div>
            <div class="online-value">{{ onlineStats.volunteerOnline || 0 }} 人</div>
          </div>
          <div class="online-chart">
            <div class="mini-bar" :style="{ height: Math.min((onlineStats.volunteerOnline || 0) * 10, 100) + 'px' }"></div>
          </div>
        </div>
        <div class="online-item">
          <div class="online-icon bg-elder-online">
            <el-icon :size="24"><UserFilled /></el-icon>
          </div>
          <div class="online-info">
            <div class="online-label">老人在线</div>
            <div class="online-value">{{ onlineStats.elderOnline || 0 }} 人</div>
          </div>
          <div class="online-chart">
            <div class="mini-bar" :style="{ height: Math.min((onlineStats.elderOnline || 0) * 10, 100) + 'px' }"></div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 图表区域 -->
    <el-row :gutter="24" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><TrendCharts /></el-icon>
                <span class="card-title">近7日每日下单数量走势</span>
              </div>
            </div>
          </template>
          <div ref="orderAmountChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><PieChart /></el-icon>
                <span class="card-title">各服务套餐订单占比</span>
              </div>
            </div>
          </template>
          <div ref="serviceTypeChart" class="chart-box pie-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><PieChart /></el-icon>
                <span class="card-title">全量订单状态占比</span>
              </div>
            </div>
          </template>
          <div ref="orderStatusChart" class="chart-box pie-chart"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#00a88d"><TrendCharts /></el-icon>
                <span class="card-title">近7日订单金额趋势</span>
              </div>
            </div>
          </template>
          <div ref="userGrowthChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷待办列表 -->
    <el-row :gutter="24" class="todo-row">
      <el-col :xs="24" :md="12">
        <el-card class="todo-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#f59e0b"><DocumentChecked /></el-icon>
                <span class="card-title">待审核志愿者申请</span>
                <el-badge :value="pendingApprovals.length" class="badge" />
              </div>
              <el-button type="primary" link @click="router.push('/admin/approval')">
                查看全部
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <div v-if="pendingApprovals.length === 0" class="empty-tip">
            <el-empty description="暂无待审核申请" :image-size="80" />
          </div>
          <div v-else class="todo-list">
            <div v-for="item in pendingApprovals.slice(0, 10)" :key="item.id" class="todo-item">
              <div class="todo-info">
                <div class="todo-name">{{ item.realName }}</div>
                <div class="todo-time">{{ formatTime(item.createTime) }}</div>
              </div>
              <div class="todo-actions">
                <el-button type="success" size="small" plain @click="goToApproval">通过</el-button>
                <el-button type="danger" size="small" plain @click="goToApproval">驳回</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card class="todo-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#3b82f6"><Bell /></el-icon>
                <span class="card-title">未处理待接单订单</span>
                <el-badge :value="pendingOrdersList.length" class="badge" />
              </div>
              <el-button type="primary" link @click="router.push('/admin/order')">
                查看全部
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <div v-if="pendingOrdersList.length === 0" class="empty-tip">
            <el-empty description="暂无待处理订单" :image-size="80" />
          </div>
          <div v-else class="todo-list">
            <div v-for="item in pendingOrdersList.slice(0, 10)" :key="item.id" class="todo-item">
              <div class="todo-info">
                <div class="todo-name">订单 {{ item.orderNo }}</div>
                <div class="todo-time">{{ formatTime(item.createTime) }}</div>
              </div>
              <el-button type="primary" size="small" plain @click="viewOrderDetail(item.id)">
                查看详情
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  getDashboardData,
  getOrderTrend7d,
  getOrderAmountTrend7d,
  getServiceTypeDist,
  searchOrder,
  getApprovalPage,
  getOnlineStats,
  getOrderStatistics
} from '@/api/admin'
import wsManager from '@/utils/websocket'
import { useAdminStore } from '@/stores/admin'
import defaultAvatar from '@/assets/default-avatar.png'
import {
  UserFilled, Bell, Document, Money, User,
  CaretTop, TrendCharts, PieChart, Check,
  ArrowRight, Connection, Warning, SuccessFilled, CircleCheck,
  Clock, Monitor, Refresh, DocumentChecked
} from '@element-plus/icons-vue'

const router = useRouter()
const adminStore = useAdminStore()

const pendingCount = ref(0)
const recentOrders = ref<any[]>([])
const pendingApprovals = ref<any[]>([])
const pendingOrdersList = ref<any[]>([])

const onlineStats = ref({
  adminOnline: 0,
  volunteerOnline: 0,
  elderOnline: 0
})

const currentDate = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const dashboardData = ref({
  elderCount: 0,
  volunteerCount: 0,
  todayOrders: 0,
  pendingOrders: 0,
  monthRevenue: 0,
  completedOrders: 0
})

const orderAmountChart = ref<HTMLDivElement | null>(null)
const serviceTypeChart = ref<HTMLDivElement | null>(null)
const orderStatusChart = ref<HTMLDivElement | null>(null)
const userGrowthChart = ref<HTMLDivElement | null>(null)

let refreshTimer: number | null = null

const getStatusName = (status: number) => {
  const names = ['待接单', '已接单', '服务中', '待确认', '已完成', '已取消', '未支付']
  return names[status] || '未知'
}

const getStatusType = (status: number) => {
  const types = ['warning', 'primary', 'primary', 'warning', 'success', 'danger', 'info']
  return types[status] || ''
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const loadDashboardData = async () => {
  try {
    const res = await getDashboardData()
    if (res.code === 200 && res.data) {
      dashboardData.value = {
        elderCount: res.data.elderCount || 0,
        volunteerCount: res.data.volunteerCount || 0,
        todayOrders: res.data.todayOrders || 0,
        pendingOrders: res.data.pendingOrders || 0,
        monthRevenue: res.data.monthRevenue || 0,
        completedOrders: res.data.completedOrders || 0
      }
      pendingCount.value = res.data.pendingCount || 0
    }

    const onlineRes = await getOnlineStats()
    if (onlineRes.code === 200 && onlineRes.data) {
      onlineStats.value = {
        adminOnline: onlineRes.data.adminCount || 0,
        volunteerOnline: onlineRes.data.volunteerCount || 0,
        elderOnline: onlineRes.data.userCount || 0
      }
    }

    const orderRes = await searchOrder(1, 5)
    if (orderRes.code === 200 && orderRes.data?.records) {
      recentOrders.value = orderRes.data.records
    }

    const pendingOrderRes = await searchOrder(1, 10, 0)
    if (pendingOrderRes.code === 200 && pendingOrderRes.data?.records) {
      pendingOrdersList.value = pendingOrderRes.data.records
    }

    const approvalRes = await getApprovalPage(1, 10, 'register', 'pending')
    if (approvalRes.code === 200 && approvalRes.data?.records) {
      pendingApprovals.value = approvalRes.data.records
    }
  } catch (error) {
    console.error('加载仪表盘数据失败', error)
  }
}

const initCharts = async () => {
  if (orderAmountChart.value) {
    const chart = echarts.init(orderAmountChart.value)
    try {
      const res = await getOrderTrend7d()
      const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
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
          data: days,
          axisLine: { lineStyle: { color: '#eee' } },
          axisLabel: { color: '#666' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f5f5f5' } },
          axisLabel: { color: '#666' }
        },
        series: [{
          name: '订单数量',
          type: 'bar',
          data: res.data || [],
          color: '#00a88d',
          barWidth: '50%',
          itemStyle: {
            borderRadius: [8, 8, 0, 0]
          }
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
            }
          },
          data: res.data?.map((item: any, index: number) => {
            const colors = ['#00a88d', '#36b9cc', '#1cc88a', '#4e73df', '#f6c23e']
            return {
              value: item.value,
              name: item.name,
              itemStyle: { color: colors[index % colors.length] }
            }
          }) || []
        }]
      })
    } catch (err) {
      console.error('获取服务分布失败', err)
    }
  }

  if (orderStatusChart.value) {
    const chart = echarts.init(orderStatusChart.value)
    try {
      const statsRes = await getOrderStatistics()
      if (statsRes.code === 200 && statsRes.data) {
        const stats = statsRes.data
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
            textStyle: { color: '#666' }
          },
          series: [{
            name: '订单状态',
            type: 'pie',
            radius: ['50%', '70%'],
            center: ['35%', '50%'],
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 3
            },
            label: { show: false },
            data: [
              { value: stats.status0Count || 0, name: '待接单', itemStyle: { color: '#f59e0b' } },
              { value: stats.status1Count || 0, name: '已接单', itemStyle: { color: '#3b82f6' } },
              { value: stats.status2Count || 0, name: '服务中', itemStyle: { color: '#10b981' } },
              { value: stats.status3Count || 0, name: '待确认', itemStyle: { color: '#8b5cf6' } },
              { value: stats.status4Count || 0, name: '已完成', itemStyle: { color: '#00a88d' } },
              { value: stats.status5Count || 0, name: '已取消', itemStyle: { color: '#ef4444' } },
              { value: stats.status6Count || 0, name: '未支付', itemStyle: { color: '#6b7280' } }
            ]
          }]
        })
      }
    } catch (err) {
      console.error('获取订单状态统计失败', err)
    }
  }

  if (userGrowthChart.value) {
    const chart = echarts.init(userGrowthChart.value)
    try {
      const res = await getOrderAmountTrend7d()
      const days = Array.from({ length: 7 }, (_, i) => {
        const d = new Date()
        d.setDate(d.getDate() - (6 - i))
        return `${d.getMonth() + 1}/${d.getDate()}`
      })
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.95)',
          textStyle: { color: '#333' },
          borderRadius: 8,
          formatter: (params: any) => {
            const data = params[0]
            return `${data.seriesName}<br/>${data.name}: ¥${data.value}`
          }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: days,
          axisLine: { lineStyle: { color: '#eee' } },
          axisLabel: { color: '#666' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f5f5f5' } },
          axisLabel: {
            color: '#666',
            formatter: (value: number) => `¥${value}`
          }
        },
        series: [{
          name: '订单金额',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: res.data || [],
          color: '#3b82f6',
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59,130,246,0.3)' },
              { offset: 1, color: 'rgba(59,130,246,0.05)' }
            ])
          },
          lineStyle: { width: 2 }
        }]
      })
    } catch (err) {
      console.error('获取七日流水数据失败', err)
    }
  }
}

const viewOrderDetail = (orderId: number) => {
  router.push(`/admin/order/detail/${orderId}`)
}

const goToApproval = () => {
  router.push('/admin/approval')
}

onMounted(() => {
  if (adminStore.adminId) {
    wsManager.connect('admin', adminStore.adminId.toString())
  }

  loadDashboardData()
  setTimeout(() => {
    initCharts()
  }, 100)

  refreshTimer = window.setInterval(() => {
    loadDashboardData()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  padding: 24px 32px;
}

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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
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
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.bg-user { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); }
.bg-user-light { background: linear-gradient(135deg, #93c5fd 0%, #bfdbfe 100%); }
.bg-volunteer { background: linear-gradient(135deg, #10b981 0%, #34d399 100%); }
.bg-volunteer-light { background: linear-gradient(135deg, #6ee7b7 0%, #a7f3d0 100%); }
.bg-order { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); }
.bg-order-light { background: linear-gradient(135deg, #fcd34d 0%, #fde68a 100%); }
.bg-revenue { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); }
.bg-completed { background: linear-gradient(135deg, #00a88d 0%, #00c4a0 100%); }
.bg-admin { background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%); }
.bg-volunteer-online { background: linear-gradient(135deg, #10b981 0%, #34d399 100%); }
.bg-elder-online { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
  margin-bottom: 4px;
}

.stat-value.currency {
  color: #f5222d;
  font-size: 20px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.trend-up { color: #10b981; }
.trend-warning { color: #f59e0b; }
.trend-success { color: #00a88d; }
.trend-stable { color: #64748b; }

.online-panel-card {
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.online-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.online-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8faf9;
  border-radius: 12px;
}

.online-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.online-info {
  flex: 1;
}

.online-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
}

.online-value {
  font-size: 20px;
  font-weight: bold;
  color: #1e293b;
}

.online-chart {
  width: 40px;
  height: 100px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.mini-bar {
  width: 24px;
  background: linear-gradient(180deg, #00a88d 0%, #00c4a0 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

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

.todo-row {
  margin-bottom: 24px;
}

.todo-card {
  height: 100%;
  border: 1px solid #e2e8f0;
}

.badge {
  margin-left: 8px;
}

.empty-tip {
  padding: 20px 0;
}

.todo-list {
  max-height: 400px;
  overflow-y: auto;
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.todo-item:last-child {
  border-bottom: none;
}

.todo-info {
  flex: 1;
}

.todo-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
}

.todo-time {
  font-size: 12px;
  color: #94a3b8;
}

.todo-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .online-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-container {
    padding: 16px;
  }
}
</style>
