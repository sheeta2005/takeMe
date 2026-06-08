<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">今日待办</h2>
      <p class="page-subtitle">处理今日需要完成的任务</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card pending-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ pendingCount }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card completed-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ completedCount }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card total-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><List /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ todoList.length }}</div>
              <div class="stat-label">总计任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办列表 -->
    <el-card class="todo-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Calendar /></el-icon>
            <span class="card-title">待办事项</span>
          </div>
        </div>
      </template>

      <el-table :data="todoList" stripe style="width: 100%">
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-checkbox v-model="row.done" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="任务内容" min-width="300">
          <template #default="{ row }">
            <span :class="{ 'completed-text': row.done }">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="120">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="todoList.length === 0" description="暂无待办事项" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, CircleCheck, List, Calendar, Delete } from '@element-plus/icons-vue'

const todoList = ref([
  { id: 1, content: '审核志愿者请假申请', done: false, priority: 'high' },
  { id: 2, content: '处理超时未接单订单', done: false, priority: 'medium' },
  { id: 3, content: '查看新用户注册申请', done: true, priority: 'low' }
])

const pendingCount = computed(() => todoList.value.filter(item => !item.done).length)
const completedCount = computed(() => todoList.value.filter(item => item.done).length)

const handleStatusChange = (row: any) => {
  if (row.done) {
    ElMessage.success('任务已完成')
  }
}

const handleDelete = (row: any) => {
  const index = todoList.value.findIndex(item => item.id === row.id)
  if (index > -1) {
    todoList.value.splice(index, 1)
    ElMessage.success('删除成功')
  }
}

const getPriorityText = (priority: string) => {
  const map: Record<string, string> = {
    high: '高优先级',
    medium: '中优先级',
    low: '低优先级'
  }
  return map[priority] || '普通'
}

const getPriorityType = (priority: string) => {
  const map: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return map[priority] || 'info'
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

.pending-card .stat-icon {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: white;
}

.completed-card .stat-icon {
  background: linear-gradient(135deg, #10b981, #34d399);
  color: white;
}

.total-card .stat-icon {
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
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

.todo-card {
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

.completed-text {
  text-decoration: line-through;
  color: var(--text-tertiary);
}
</style>
