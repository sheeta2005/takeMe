<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息中心</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">消息类型</label>
        <el-select v-model="filterType" placeholder="请选择类型" @change="fetchMsgs" clearable>
          <el-option label="全部" value="" />
          <el-option label="系统通知" :value="0" />
          <el-option label="服务通知" :value="1" />
          <el-option label="温馨提醒" :value="2" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">状态</label>
        <el-select v-model="filterStatus" placeholder="请选择状态" @change="fetchMsgs" clearable>
          <el-option label="全部" value="" />
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
        </el-select>
      </div>

      <el-button type="primary" @click="fetchMsgs">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 消息列表卡片 -->
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
                  <span class="msg-title" :class="{ unread: !msg.isRead }">{{ msg.title }}</span>
                  <el-icon v-if="!msg.isRead" class="unread-icon"><Bell /></el-icon>
                </div>
                <div class="msg-time">{{ msg.createTime }}</div>
              </div>
              <div class="msg-preview">{{ msg.content }}</div>
            </div>
            <div class="msg-actions">
              <el-button
                v-if="!msg.isRead"
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

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:currentPage="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20]"
        layout="total, sizes, prev, pager, next"
        @change="fetchMsgs"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import type { Message } from '@/types/Message.ts'

const router = useRouter()

// 筛选条件
const filterType = ref<string | number>('')
const filterStatus = ref<boolean | string>('')

// 分页配置
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 列表数据
const msgList = ref<Message[]>([])

onMounted(() => {
  fetchMsgs()
})

// 获取消息列表
const fetchMsgs = async () => {
  loading.value = true
  try {
    // 模拟数据（保留你的原有逻辑）
    msgList.value = [
      {
        id: 1,
        userId: 2001,
        type: 1,
        title: '服务提醒：助餐服务',
        content: '您的助餐服务订单ORD20260520001将于明天09:00送达，请您准备接收。',
        createTime: '2026-05-19 10:30:00',
        isRead: false,
        relatedId: 'ORD20260520001',
        relatedUrl: '/user/order/detail/ORD20260520001'
      },
      {
        id: 2,
        userId: 2001,
        type: 0,
        title: '平台通知：服务更新',
        content: '平台已更新助洁服务流程，现在可以预约周末服务了，欢迎体验。',
        createTime: '2026-05-18 14:00:00',
        isRead: true,
        relatedId: '',
        relatedUrl: ''
      },
      {
        id: 3,
        userId: 2001,
        type: 2,
        title: '温馨提醒：天气变化',
        content: '未来三天有降雨，您的服务可能会调整时间，请留意订单通知。',
        createTime: '2026-05-17 11:00:00',
        isRead: false,
        relatedId: '',
        relatedUrl: ''
      }
    ]
    total.value = 3
  } catch (err) {
    console.error('获取消息失败', err)
    ElMessage.error('获取消息列表失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
const resetFilter = () => {
  filterType.value = ''
  filterStatus.value = ''
  currentPage.value = 1
  fetchMsgs()
}

// 跳转到消息详情
const goToDetail = (msg: Message) => {
  if (!msg.id) return
  router.push(`/user/message/detail/${msg.id}`)
}

// 标记已读
const handleMarkRead = (msg: Message) => {
  msg.isRead = true
  ElMessage.success('标记已读成功')
}

// 类型映射
const getTypeText = (type: number): string => {
  const map = { 0: '系统通知', 1: '服务通知', 2: '温馨提醒' }
  return map[type] || '未知'
}

const getTypeTagType = (type: number): string => {
  const map = { 0: 'primary', 1: 'warning', 2: 'success' }
  return map[type] || ''
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
