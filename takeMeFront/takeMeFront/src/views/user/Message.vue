<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">消息中心</h2>
        <p class="page-subtitle">查看您的系统通知和消息</p>
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
        <el-form-item label="状态">
          <el-select v-model="filterIsRead" placeholder="请选择" @change="fetchMsgs" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
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
            <span class="card-title">我的消息</span>
          </div>
          <span class="total-count">共 {{ total }} 条</span>
        </div>
      </template>

      <el-table :data="msgList" v-loading="loading" stripe style="width: 100%" @row-click="handleRowClick">
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="msg-title-cell">
              <el-tag :type="getTypeTagType(row.type)" size="small" class="type-tag">
                {{ getTypeText(row.type) }}
              </el-tag>
              <span class="msg-title" :class="{ unread: row.isRead === 0 }">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isRead === 0" type="danger" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="接收时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleMarkRead(row)">
              <el-icon><Check /></el-icon>
              {{ row.isRead === 0 ? '标记已读' : '查看详情' }}
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Bell, Check } from '@element-plus/icons-vue'
import { getUserMessages, markMessageRead } from '@/api/user'

const router = useRouter()

const loading = ref(false)
const filterType = ref<number | ''>('')
const filterIsRead = ref<number | ''>('')

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
    const res = await getUserMessages({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      type: filterType.value !== '' ? filterType.value : undefined,
      isRead: filterIsRead.value !== '' ? filterIsRead.value : undefined
    })
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
  filterIsRead.value = ''
  currentPage.value = 1
  fetchMsgs()
}

const handleRowClick = (row: any) => {
  if (!row.id) return
  if (row.isRead === 0) {
    markMessageRead(row.id).then(() => {
      row.isRead = 1
    }).catch(() => {})
  }
  router.push(`/user/message/detail/${row.id}`)
}

const handleMarkRead = async (row: any) => {
  if (!row.id) return
  if (row.isRead === 0) {
    try {
      await markMessageRead(row.id)
      row.isRead = 1
      ElMessage.success('标记已读成功')
    } catch (err) {
      console.error('标记已读失败', err)
      ElMessage.error('操作失败')
    }
  } else {
    router.push(`/user/message/detail/${row.id}`)
  }
}

const getTypeText = (type: number): string => {
  const map: Record<number, string> = { 0: '系统通知', 1: '任务通知', 2: '温馨提醒' }
  return map[type] || '未知'
}

const getTypeTagType = (type: number): string => {
  const map: Record<number, string> = { 0: 'info', 1: 'primary', 2: 'success' }
  return map[type] || ''
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

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

.msg-title.unread {
  font-weight: 600;
}

.content-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.time-text {
  color: var(--text-secondary);
  font-size: 13px;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
