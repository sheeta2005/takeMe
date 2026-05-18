<template>
  <div class="workbench">
    <!-- 顶部：标题 + 导出Excel按钮 -->
    <div class="top-bar">
      <h2>工作台</h2>
      <el-button type="warning" @click="handleExport"> 导出统计数据 </el-button>
    </div>

    <!-- 今日数据 -->
    <div class="data-card">
      <div class="data-title">今日数据 {{ date }}</div>
      <div class="data-grid">
        <div class="data-item">
          <div class="item-label">营业额</div>
          <div class="item-value">¥ {{ data.turnover || 0 }}</div>
        </div>
        <div class="data-item">
          <div class="item-label">有效订单</div>
          <div class="item-value">{{ data.validOrder || 0 }}</div>
        </div>
        <div class="data-item">
          <div class="item-label">订单完成率</div>
          <div class="item-value">{{ data.orderRate || 0 }}%</div>
        </div>
        <div class="data-item">
          <div class="item-label">平均客单价</div>
          <div class="item-value">¥ {{ data.avgPrice || 0 }}</div>
        </div>
        <div class="data-item">
          <div class="item-label">新增用户</div>
          <div class="item-value">{{ data.newUser || 0 }}</div>
        </div>
      </div>
      <div class="detail-link">详细数据 ></div>
    </div>

    <!-- ECharts 图表区域（苍穹外卖同款） -->
    <div class="chart-section">
      <div class="chart-box">
        <div class="chart-title">营业额趋势</div>
        <div id="turnoverChart" style="width: 100%; height: 280px"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">用户统计</div>
        <div id="userChart" style="width: 100%; height: 280px"></div>
      </div>
    </div>

    <!-- 订单管理 -->
    <div class="order-card">
      <div class="order-title">订单管理 {{ date }}</div>
      <div class="order-grid">
        <div class="order-item">待接单</div>
        <div class="order-item">待派送</div>
        <div class="order-item">已完成</div>
        <div class="order-item">已取消</div>
        <div class="order-item">全部订单</div>
      </div>
      <div class="detail-link">订单明细 ></div>
    </div>

    <!-- 服务总览 + 志愿者总览 -->
    <div class="service-card">
      <div class="service-left">
        <div class="service-title">服务总览</div>
        <div class="service-grid">
          <div class="service-item">
            <div class="item-icon">📍</div>
            <div class="item-text">已接单</div>
          </div>
          <div class="service-item">
            <div class="item-icon">🚚</div>
            <div class="item-text">已派送</div>
          </div>
        </div>
        <el-button type="primary" class="add-btn">新增服务</el-button>
      </div>
      <div class="service-right">
        <div class="service-title">志愿者总览</div>
        <div class="service-grid">
          <div class="service-item">
            <div class="item-icon">✅</div>
            <div class="item-text">在线</div>
          </div>
          <div class="service-item">
            <div class="item-icon">❌</div>
            <div class="item-text">离线</div>
          </div>
        </div>
        <el-button type="primary" class="add-btn">新增志愿者</el-button>
      </div>
    </div>

    <!-- 订单信息 -->
    <div class="order-info">
      <div class="info-title">订单信息</div>
      <div class="info-tabs">
        <el-button type="primary">待接单</el-button>
        <el-button>待派送</el-button>
      </div>
      <el-table :data="orderList" border>
        <el-table-column prop="id" label="订单ID" />
        <el-table-column prop="userName" label="老人姓名" />
        <el-table-column prop="volunteerName" label="志愿者" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="createTime" label="下单时间" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getWorkbenchData, exportStatisticsExcel } from '@/api/index'
import { ElMessage } from 'element-plus'

const date = ref(new Date().toLocaleDateString())

const data = ref({
  turnover: 0,
  validOrder: 0,
  orderRate: 0,
  avgPrice: 0,
  newUser: 0,
})

const orderList = ref([])

let turnoverChart: any
let userChart: any

onMounted(async () => {
  const res = await getWorkbenchData()
  data.value = res.data.workbench
  orderList.value = res.data.orderList
  initCharts(res.data.chart)
})

const initCharts = (chartData: any) => {
  turnoverChart = echarts.init(document.getElementById('turnoverChart')!)
  userChart = echarts.init(document.getElementById('userChart')!)

  turnoverChart.setOption({
    xAxis: { type: 'category', data: chartData.dates },
    yAxis: { type: 'value' },
    series: [
      {
        data: chartData.turnoverList,
        type: 'line',
        smooth: true,
        itemStyle: { color: '#ff9501' },
      },
    ],
  })

  userChart.setOption({
    xAxis: { type: 'category', data: chartData.dates },
    yAxis: { type: 'value' },
    series: [
      {
        data: chartData.newUserList,
        type: 'bar',
        itemStyle: { color: '#ff9501' },
      },
    ],
  })
}

// 改名，不和接口重名
const handleExport = async () => {
  try {
    const res = await exportStatisticsExcel()
    const blob = new Blob([res], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `志愿平台统计数据_${date.value}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error('导出失败')
  }
}
</script>

<style scoped>
.workbench {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.data-card,
.order-card,
.service-card,
.order-info,
.chart-box {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.data-title,
.order-title,
.service-title,
.info-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.data-grid,
.order-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin: 20px 0;
}

.data-item {
  background: #fef8f0;
  padding: 20px;
  border-radius: 10px;
  border-left: 4px solid #ff9501;
}

.order-item {
  background: #f7f8fa;
  padding: 20px;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
}
.order-item:hover {
  background: #e9f0ff;
}

.chart-section {
  display: flex;
  gap: 20px;
}
.chart-box {
  flex: 1;
}
.chart-title {
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 15px;
}

.service-card {
  display: flex;
  gap: 20px;
}
.service-left,
.service-right {
  flex: 1;
}
.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.service-item {
  background: #f7f8fa;
  padding: 18px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.item-icon {
  font-size: 20px;
}
.add-btn {
  float: right;
  margin-top: 10px;
}

.detail-link {
  text-align: right;
  color: #999;
  cursor: pointer;
}

.info-tabs {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}
</style>
