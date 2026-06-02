<template>
  <div class="message-container">
    <h2 class="page-title">消息中心</h2>

    <!-- 筛选条件 -->
    <div class="filter-box">
      <el-select v-model="filterType" placeholder="消息类型" clearable style="width: 150px; margin-right: 10px">
        <el-option label="全部" :value="null" />
        <el-option label="系统通知" :value="0" />
        <el-option label="订单消息" :value="1" />
        <el-option label="审核消息" :value="2" />
      </el-select>

      <el-select v-model="filterRead" placeholder="阅读状态" clearable style="width: 150px; margin-right: 10px">
        <el-option label="全部" :value="null" />
        <el-option label="未读" :value="0" />
        <el-option label="已读" :value="1" />
      </el-select>

      <el-button type="primary" @click="loadMessages">查询</el-button>
    </div>

    <!-- 消息列表 -->
    <div class="message-list">
      <el-empty v-if="messageList.length === 0" description="暂无消息" />

      <div
        v-for="msg in messageList"
        :key="msg.id"
        class="message-item"
        :class="{ unread: msg.isRead === 0 }"
        @click="handleMessageClick(msg)"
      >
        <div class="message-header">
          <span class="message-title">{{ msg.title }}</span>
          <el-tag v-if="msg.isRead === 0" type="danger" size="small">未读</el-tag>
        </div>
        <div class="message-content">{{ msg.content }}</div>
        <div class="message-footer">
          <span class="message-time">{{ msg.createTime }}</span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-box" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVolunteerMessages, markVolunteerMessageRead } from '@/api/volunteer'

const router = useRouter()

// 筛选条件
const filterType = ref<number | null>(null)
const filterRead = ref<number | null>(null)

// 消息列表
const messageList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载消息列表
const loadMessages = async () => {
  try {
    const res = await getVolunteerMessages({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      type: filterType.value === '' ? undefined : Number(filterType.value),
      isRead: filterRead.value === '' ? undefined : Number(filterRead.value)
    })

    if (res.code === 200) {
      messageList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    ElMessage.error('获取消息列表失败')
  }
}

// 分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadMessages()
}

// 点击消息
const handleMessageClick = async (msg: any) => {
  // 标记为已读
  if (msg.isRead === 0) {
    try {
      await markVolunteerMessageRead(msg.id)
      msg.isRead = 1
    } catch {
      ElMessage.error('标记已读失败')
    }
  }

  // 跳转到消息详情
  router.push(`/volunteer/message/detail/${msg.id}`)
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.message-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 30px 0;
  text-align: center;
}

.filter-box {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message-item:hover {
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.15);
  transform: translateY(-2px);
}

.message-item.unread {
  border-left: 4px solid #00b899;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.message-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.message-content {
  font-size: 15px;
  color: #666;
  margin-bottom: 10px;
  line-height: 1.6;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-time {
  font-size: 13px;
  color: #999;
}

.pagination-box {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
