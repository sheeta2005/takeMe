<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息中心</h2>
      <el-button type="primary" @click="$router.push('/admin/message/send')">发送消息</el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">消息类型</label>
        <el-select v-model="filterType" placeholder="请选择类型" @change="fetchMsgs">
          <el-option label="全部" value="" />
          <el-option label="系统通知" :value="0" />
          <el-option label="任务通知" :value="1" />
          <el-option label="温馨提醒" :value="2" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">接收者类型</label>
        <el-select v-model="filterReceiverType" placeholder="请选择" @change="fetchMsgs">
          <el-option label="全部" value="" />
          <el-option label="老人用户" :value="0" />
          <el-option label="志愿者" :value="1" />
          <el-option label="全部广播" :value="2" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">时间范围</label>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="fetchMsgs"
        />
      </div>

      <el-button type="primary" @click="fetchMsgs">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 消息列表卡片 -->
    <div class="list-card">
      <el-list :data="msgList" class="msg-list">
        <el-list-item v-for="msg in msgList" :key="msg.id" class="msg-item">
          <template #default>
            <div class="msg-content">
              <div class="msg-header">
                <div class="msg-title-wrapper">
                  <el-tag
                    v-if="msg.type !== undefined"
                    :type="getTypeTagType(msg.type)"
                    size="small"
                    class="type-tag"
                  >
                    {{ getTypeText(msg.type) }}
                  </el-tag>
                  <span class="msg-title">{{ msg.title }}</span>
                </div>
                <div class="msg-meta">
                  <span class="receiver-type">{{ getReceiverTypeText(msg.receiverType) }}</span>
                  <span class="msg-time">{{ msg.createTime }}</span>
                </div>
              </div>
              <div class="msg-preview">{{ msg.content }}</div>
            </div>
            <div class="msg-actions">
              <el-button type="danger" link @click="handleDelete(msg)">删除</el-button>
            </div>
          </template>
        </el-list-item>
      </el-list>

      <!-- 空状态 -->
      <el-empty v-if="msgList.length === 0" description="暂无消息" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchMsgs"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSentMessagePage, deleteMessage } from '@/api/admin'

const filterType = ref<number | ''>('')
const filterReceiverType = ref<number | ''>('')
const filterDateRange = ref<string[]>([])

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const msgList = ref<any[]>([])

onMounted(() => {
  fetchMsgs()
})

const fetchMsgs = async () => {
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
  }
}

const resetFilter = () => {
  filterType.value = ''
  filterReceiverType.value = ''
  filterDateRange.value = []
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
.page-container {
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

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.list-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
  margin-bottom: 20px;
}

.msg-list {
  width: 100%;
}

.msg-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}

.msg-item:last-child {
  border-bottom: none;
}

.msg-content {
  flex: 1;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.msg-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-tag {
  flex-shrink: 0;
}

.msg-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.msg-meta {
  display: flex;
  gap: 16px;
  align-items: center;
}

.receiver-type {
  font-size: 12px;
  color: #999;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

.msg-time {
  font-size: 14px;
  color: #999;
}

.msg-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-actions {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
</style>
