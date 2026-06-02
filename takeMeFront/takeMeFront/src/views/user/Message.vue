<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息中心</h2>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">消息类型</label>
        <el-select v-model="filterType" placeholder="请选择类型" @change="fetchMsgs" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="系统通知" :value="0" />
          <el-option label="订单通知" :value="1" />
          <el-option label="服务通知" :value="2" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">状态</label>
        <el-select v-model="filterIsRead" placeholder="请选择状态" @change="fetchMsgs" clearable>
          <el-option label="全部" :value="undefined" />
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
      </div>

      <el-button type="primary" @click="fetchMsgs">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <div class="list-card" v-loading="loading">
      <el-list :data="msgList" class="msg-list">
        <el-list-item
          v-for="msg in msgList"
          :key="msg.id"
          class="msg-item"
          @click="goToDetail(msg)"
        >
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
                  <span class="msg-title" :class="{ unread: msg.isRead === 0 }">{{ msg.title }}</span>
                  <el-icon v-if="msg.isRead === 0" class="unread-icon"><Bell /></el-icon>
                </div>
                <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
              </div>
              <div class="msg-preview">{{ msg.content }}</div>
            </div>
            <div class="msg-actions">
              <el-button
                v-if="msg.isRead === 0"
                type="primary"
                link
                @click.stop="handleMarkRead(msg)"
              >
                标记已读
              </el-button>
            </div>
          </template>
        </el-list-item>
      </el-list>

      <el-empty v-if="msgList.length === 0 && !loading" description="暂无消息记录" :image-size="120" />
    </div>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchMsgs"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { getUserMessages, markMessageRead } from '@/api/user'

const router = useRouter()

const filterType = ref<number | undefined>(undefined)
const filterIsRead = ref<number | undefined>(undefined)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

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
      type: filterType.value,
      isRead: filterIsRead.value
    })

    if (res.code === 200 && res.data) {
      msgList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      msgList.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取消息失败', err)
    ElMessage.error('获取消息列表失败')
    msgList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterType.value = undefined
  filterIsRead.value = undefined
  currentPage.value = 1
  fetchMsgs()
}

const goToDetail = (msg: any) => {
  if (!msg.id) return
  router.push(`/user/message/detail/${msg.id}`)
}

const handleMarkRead = async (msg: any) => {
  try {
    await markMessageRead(msg.id)
    msg.isRead = 1
    ElMessage.success('标记已读成功')
  } catch (err) {
    console.error('标记已读失败', err)
    ElMessage.error('操作失败')
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchMsgs()
}

const getTypeText = (type: number): string => {
  const map: Record<number, string> = { 0: '系统通知', 1: '订单通知', 2: '服务通知' }
  return map[type] || '未知'
}

const getTypeTagType = (type: number): string => {
  const map: Record<number, string> = { 0: 'primary', 1: 'warning', 2: 'success' }
  return map[type] || ''
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 12px 0;
}

.header-row {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  background: #ffffff;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 184, 153, 0.08);
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #4b5563;
  font-weight: 500;
}

.list-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 184, 153, 0.08);
  padding: 16px 24px;
  margin-bottom: 20px;
  min-height: 200px;
}

.msg-list {
  width: 100%;
}

.msg-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: all 0.2s ease;
}

.msg-item:last-child {
  border-bottom: none;
}

.msg-item:hover {
  background-color: #f9fafb;
  padding-left: 8px;
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
  color: #374151;
}

.msg-title.unread {
  color: #111827;
  font-weight: 600;
}

.unread-icon {
  color: #f56c6c;
  font-size: 16px;
}

.msg-time {
  font-size: 14px;
  color: #9ca3af;
}

.msg-preview {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-actions {
  flex-shrink: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 184, 153, 0.08);
}
</style>
