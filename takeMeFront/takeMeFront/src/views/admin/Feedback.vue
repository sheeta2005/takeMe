<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">差评管理</h2>
          <p class="page-subtitle">查看和处理用户差评反馈</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card bad-feedback-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ feedbackList.length }}</div>
              <div class="stat-label">差评总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card pending-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ pendingCount }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card resolved-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ resolvedCount }}</div>
              <div class="stat-label">已处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 差评列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#ef4444"><ChatLineRound /></el-icon>
            <span class="card-title">差评列表</span>
          </div>
          <span class="total-count">共 {{ feedbackList.length }} 条</span>
        </div>
      </template>

      <el-table :data="feedbackList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="orderId" label="订单号" width="150" />
        <el-table-column prop="user" label="评价用户" width="120" />
        <el-table-column prop="rating" label="评分" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="差评内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'resolved' ? 'success' : 'warning'" size="small">
              {{ row.status === 'resolved' ? '已处理' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button
              v-if="row.status !== 'resolved'"
              type="success"
              link
              size="small"
              @click="handleResolve(row)"
            >
              <el-icon><Check /></el-icon>
              标记处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="feedbackList.length === 0" description="暂无差评记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleClose, Warning, CircleCheck, ChatLineRound, View, Check } from '@element-plus/icons-vue'

const loading = ref(false)

const feedbackList = ref([
  {
    orderId: 'ORD001',
    user: '李奶奶',
    rating: 1,
    content: '送餐延迟了1小时，饭菜都凉了',
    time: '2026-05-24 14:30:00',
    status: 'pending'
  },
  {
    orderId: 'ORD002',
    user: '张爷爷',
    rating: 2,
    content: '志愿者服务态度一般',
    time: '2026-05-23 10:15:00',
    status: 'resolved'
  }
])

const pendingCount = computed(() => feedbackList.value.filter(item => item.status === 'pending').length)
const resolvedCount = computed(() => feedbackList.value.filter(item => item.status === 'resolved').length)

const handleViewDetail = (row: any) => {
  // 跳转到订单详情或显示弹窗
  ElMessage.info(`查看订单 ${row.orderId} 的详情`)
}

const handleResolve = (row: any) => {
  row.status = 'resolved'
  ElMessage.success('已标记为处理完成')
}
</script>

<style scoped>
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: 1px solid var(--border-light);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bad-feedback-card .stat-icon {
  background: linear-gradient(135deg, #ef4444, #f87171);
  color: white;
}

.pending-card .stat-icon {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: white;
}

.resolved-card .stat-icon {
  background: linear-gradient(135deg, #10b981, #34d399);
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.table-card {
  border: 1px solid var(--border-light);
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
  color: var(--text-primary);
}

.total-count {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
