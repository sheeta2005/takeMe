<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">消息中心</h2>
          <p class="page-subtitle">管理系统消息通知</p>
        </div>
        <el-button type="primary" size="large" @click="$router.push('/admin/sendMsg')">
          <el-icon><ChatDotRound /></el-icon>
          发送消息
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="消息类型">
          <el-select v-model="filterType" placeholder="请选择类型" @change="fetchMsgs" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="系统通知" :value="0" />
            <el-option label="任务通知" :value="1" />
            <el-option label="温馨提醒" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收者类型">
          <el-select v-model="filterReceiverType" placeholder="请选择" @change="fetchMsgs" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="老人用户" :value="0" />
            <el-option label="志愿者" :value="1" />
            <el-option label="全部广播" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchMsgs">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 消息列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Bell /></el-icon>
            <span class="card-title">已发送消息</span>
          </div>
          <span class="total-count">共 {{ total }} 条</span>
        </div>
      </template>

      <el-table :data="msgList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="msg-title-cell">
              <el-tag :type="getTypeTagType(row.type)" size="small" class="type-tag">
                {{ getTypeText(row.type) }}
              </el-tag>
              <span class="msg-title">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="接收者类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">
              {{ getReceiverTypeText(row.receiverType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchMsgs"
        style="margin-top: 24px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Bell, ChatDotRound, Delete } from '@element-plus/icons-vue'
import { getSentMessagePage, deleteMessage } from '@/api/admin'

const loading = ref(false)
const filterType = ref<number | ''>('')
const filterReceiverType = ref<number | ''>('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const msgList = ref<any[]>([])

onMounted(() => {
  fetchMsgs()
})

const fetchMsgs = async () => {
  loading.value = true
  try {
    const res = await getSentMessagePage(
      currentPage.value,
      pageSize.value,
      filterReceiverType.value !== '' ? filterReceiverType.value : undefined,
      filterType.value !== '' ? filterType.value : undefined
    )
    msgList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取消息列表失败', err)
    ElMessage.error('获取消息列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterType.value = ''
  filterReceiverType.value = ''
  currentPage.value = 1
  fetchMsgs()
}

const handleDelete = (msg: any) => {
  ElMessageBox.confirm('确定要删除该消息吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMessage(msg.id)
      ElMessage.success('删除成功')
      fetchMsgs()
    } catch (err) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const getTypeText = (type: number) => {
  const map = ['系统通知', '任务通知', '温馨提醒']
  return map[type] || '未知'
}

const getTypeTagType = (type: number) => {
  const map = ['info', 'primary', 'success']
  return map[type] || 'info'
}

const getReceiverTypeText = (type: number) => {
  const map = ['老人用户', '志愿者', '全部广播']
  return map[type] || '未知'
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
}

.filter-form {
  margin-bottom: 0;
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

.msg-title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-tag {
  flex-shrink: 0;
}

.msg-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.content-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.time-text {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
