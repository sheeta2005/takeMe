<template>
  <div class="dashboard-container">
    <!-- 标题 + 实时日期 -->
    <div class="header-row">
      <h2 class="page-title">工作台</h2>
      <div class="date-display">{{ currentDate }}</div>
    </div>

    <!-- 数据统计卡片 -->
    <div class="card-list">
      <div class="stat-card">
        <div class="card-title">总订单数</div>
        <div class="card-value">{{ dashboardData.totalOrders }}</div>
        <div class="card-trend positive">↑ 12% 较上周</div>
      </div>

      <!-- 可点击跳转 服务中订单 -->
      <div class="stat-card" @click="goToActiveOrders">
        <div class="card-title">服务中订单</div>
        <div class="card-value">{{ dashboardData.activeOrders }}</div>
        <div class="card-trend neutral">稳定</div>
      </div>

      <div class="stat-card">
        <div class="card-title">今日营业额</div>
        <div class="card-value">¥{{ dashboardData.todayRevenue }}</div>
        <div class="card-trend positive">↑ 8% 较昨日</div>
      </div>

      <div class="stat-card">
        <div class="card-title">志愿者人数</div>
        <div class="card-value">{{ dashboardData.volunteerCount }}</div>
        <div class="card-trend positive">+3 本月新增</div>
      </div>

      <div class="stat-card">
        <div class="card-title">老人用户数</div>
        <div class="card-value">{{ dashboardData.elderCount }}</div>
        <div class="card-trend positive">+15 本月新增</div>
      </div>

      <div class="stat-card">
        <div class="card-title">本月积分发放</div>
        <div class="card-value">{{ dashboardData.pointsIssued }}</div>
        <div class="card-trend positive">↑ 25% 较上月</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-row">
      <div class="chart-card">
        <h3 class="chart-title">订单趋势（近7天）</h3>
        <div ref="orderTrendChart" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <h3 class="chart-title">服务类型分布</h3>
        <div ref="serviceTypeChart" class="chart-box"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
// 保留接口导入
import { getDashboardData, getOrderTrend7d, getServiceTypeDist } from '@/api/admin'

const router = useRouter()

// 实时日期
const currentDate = computed(() => {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${week[now.getDay()]}`
})

// 模拟数据（前端兜底）
const dashboardData = ref({
  totalOrders: 128,
  activeOrders: 12,
  todayRevenue: 3280,
  volunteerCount: 45,
  elderCount: 210,
  pointsIssued: 15600
})

// 图表DOM
const orderTrendChart = ref<HTMLDivElement | null>(null)
const serviceTypeChart = ref<HTMLDivElement | null>(null)

// 跳转方法
const goToActiveOrders = () => {
  router.push({
    path: '/admin/order',
    query: { status: 'active' }
  })
}

// 获取近7天日期
const getRecent7Days = () => {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    days.push(`${date.getMonth() + 1}/${date.getDate()}`)
  }
  return days
}

// 初始化图表
const initCharts = () => {
  // 订单趋势图
  if (orderTrendChart.value) {
    const chart = echarts.init(orderTrendChart.value)
    // 保留接口调用代码，仅注释执行逻辑
    /*
    getOrderTrend7d().then(res => {
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.95)',
          textStyle: { color: '#333' }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: getRecent7Days(),
          axisLine: { lineStyle: { color: '#eee' } }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f5f5f5' } }
        },
        series: [{
          name: '订单数',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 7,
          data: res.data,
          color: '#00a88d',
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0,168,141,0.25)' },
              { offset: 1, color: 'rgba(0,168,141,0)' }
            ])
          }
        }]
      })
    }).catch(err => console.error('获取订单趋势失败', err))
    */
    // 模拟数据渲染
    chart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        textStyle: { color: '#333' }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: getRecent7Days(),
        axisLine: { lineStyle: { color: '#eee' } }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f5f5f5' } }
      },
      series: [{
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: [12, 19, 15, 22, 28, 18, 25],
        color: '#00a88d',
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,168,141,0.25)' },
            { offset: 1, color: 'rgba(0,168,141,0)' }
          ])
        }
      }]
    })
  }

  // 服务类型饼图
  if (serviceTypeChart.value) {
    const chart = echarts.init(serviceTypeChart.value)
    // 保留接口调用代码，仅注释执行逻辑
    /*
    getServiceTypeDist().then(res => {
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', right: 20, top: 'center', textStyle: { color: '#666' } },
        series: [{
          name: '服务类型',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
          data: res.data
        }]
      })
    }).catch(err => console.error('获取服务分布失败', err))
    */
    // 模拟数据渲染
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 20, top: 'center', textStyle: { color: '#666' } },
      series: [{
        name: '服务类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
        data: [
          { value: 35, name: '助餐服务', itemStyle: { color: '#00a88d' } },
          { value: 25, name: '助洁服务', itemStyle: { color: '#36b9cc' } },
          { value: 20, name: '助医服务', itemStyle: { color: '#1cc88a' } },
          { value: 20, name: '代购服务', itemStyle: { color: '#4e73df' } }
        ]
      }]
    })
  }
}

// 页面加载
onMounted(() => {
  // 保留接口调用代码，仅注释执行逻辑
  /*
  getDashboardData().then(res => {
    dashboardData.value = res.data
  }).catch(err => console.error('获取工作台数据失败', err))
  */
  initCharts()
})

onUnmounted(() => {
  if (orderTrendChart.value) echarts.dispose(orderTrendChart.value)
  if (serviceTypeChart.value) echarts.dispose(serviceTypeChart.value)
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  padding: 10px 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.date-display {
  font-size: 15px;
  color: #666;
  background: #f6f6f6;
  padding: 6px 14px;
  border-radius: 20px;
}

.card-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 26px 20px;
  box-shadow: 0 4px 12px rgba(0, 168, 141, 0.08);
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 168,141,0.12);
}

.card-title {
  font-size: 16px;
  color: #666;
  margin-bottom: 10px;
}

.card-value {
  font-size: 34px;
  font-weight: bold;
  color: #00a88d;
  margin-bottom: 8px;
}

.card-trend {
  font-size: 13px;
}

.positive {
  color: #10b981;
}

.neutral {
  color: #888;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.chart-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 168, 141, 0.08);
}

.chart-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 18px;
  font-weight: 600;
}

.chart-box {
  width: 100%;
  height: 320px;
}
</style>
