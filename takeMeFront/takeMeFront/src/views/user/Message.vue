<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息中心</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">消息类型</label>
        <el-select v-model="filterType" placeholder="请选择类型" @change="fetchMsgs">
          <el-option label="全部" value="" />
          <el-option label="系统通知" :value="0" />
          <el-option label="服务通知" :value="1" />
          <el-option label="温馨提醒" :value="2" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">状态</label>
        <el-select v-model="filterStatus" placeholder="请选择状态" @change="fetchMsgs">
          <el-option label="全部" value="" />
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
        </el-select>
      </div>

      <el-button type="primary" @click="fetchMsgs">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 消息列表卡片 -->
    <div class="list-card">
      <el-list :data="msgList" class="msg-list">
        <el-list-item v-for="msg in msgList" :key="msg.id" class="msg-item" @click="goToDetail(msg)">
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

      <el-empty v-if="msgList.length === 0" description="暂无消息" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
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
// import { getUserMessages } from '@/api/user'
import type { message } from '@/types/message'

const router = useRouter()

// 筛选
const filterType = ref('')
const filterStatus = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 列表数据
const msgList = ref<message[]>([])

onMounted(() => {
  fetchMsgs()
})

// 获取消息列表（接口已注释，后端优先返回未读消息）
const fetchMsgs = async () => {
  try {
    // --- 后端接口调用（已注释） ---
    /*
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      type: filterType.value || undefined,
      isRead: filterStatus.value === '' ? undefined : filterStatus.value
    }
    const res = await getUserMessages(params)
    msgList.value = res.data.list
    total.value = res.data.total
    */

    // --- 模拟数据 ---
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
        isRead: true
      },
      {
        id: 3,
        userId: 2001,
        type: 2,
        title: '温馨提醒：天气变化',
        content: '未来三天有降雨，您的服务可能会调整时间，请留意订单通知。',
        createTime: '2026-05-17 11:00:00',
        isRead: false
      }
    ]
    total.value = 3
  } catch (err) {
    console.error('获取消息失败', err)
    ElMessage.error('获取消息列表失败')
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
const goToDetail = (msg: message) => {
  router.push(`/user/message/detail/${msg.id}`)
}

// 标记已读
const handleMarkRead = (msg: message) => {
  ElMessage.success('已标记为已读')
  msg.isRead = true
}

// 类型映射
const getTypeText = (type: number) => {
  const map: Record<number, string> = {
    0: '系统通知',
    1: '服务通知',
    2: '温馨提醒'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type: number) => {
  const map: Record<number, string> = {
    0: 'primary',
    1: 'warning',
    2: 'success'
  }
  return map[type] || ''
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 10px 0;
}

.header-row {
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
  box-shadow: 0 2px 12px rgba(0, 184, 153, 0.06);
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
  box-shadow: 0 2px 12px rgba(0, 184, 153, 0.06);
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
  cursor: pointer;
}

.msg-item:last-child {
  border-bottom: none;
}

.msg-item:hover {
  background-color: #fafafa;
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

.msg-title.unread {
  color: #222;
  font-weight: 600;
}

.unread-icon {
  color: #f56c6c;
  font-size: 16px;
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
  box-shadow: 0 2px 12px rgba(0, 184, 153, 0.06);
}
</style>
