<template>
  <div class="read-message-container">
    <div class="header">
      <el-button type="text" @click="back">← 返回</el-button>
      <h2 class="page-title">已读消息</h2>
    </div>

    <div class="read-list">
      <!-- ✅ 点击跳转到消息详情页 -->
      <div class="message-item" v-for="msg in readMessages" :key="msg.id" @click="goToDetail(msg.id)">
        <div class="msg-icon">
          <span v-if="msg.type === 'system'">📢</span>
          <span v-else-if="msg.type === 'task'">📩</span>
          <span v-else>⏰</span>
        </div>
        <div class="msg-content">
          <div class="msg-title">{{ msg.title }}</div>
          <div class="msg-text">{{ msg.content }}</div>
          <div class="msg-time">{{ msg.createTime }}</div>
        </div>
      </div>

      <div class="empty-tip" v-if="readMessages.length === 0">
        暂无已读消息
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { MessageInfo } from '@/types/message'

const router = useRouter()
const messageList = ref<MessageInfo[]>([])

// 过滤已读消息
const readMessages = computed(() => messageList.value.filter(msg => msg.isRead))

// ✅ 统一跳转到消息详情页
const goToDetail = (id: number) => {
  router.push(`/volunteer/message/detail/${id}`)
}

// 返回
const back = () => {
  router.go(-1)
}

const fetchMessageList = async () => {
  messageList.value = [
    {
      id: 1,
      type: 'task',
      title: '新任务通知',
      content: '您有一个新的助餐服务任务，订单号：ORD20260520001，请及时处理',
      createTime: '2026-05-20 10:30',
      isRead: true,
      relatedId: 'ORD20260520001',
      relatedUrl: '/volunteer/order/ORD20260520001'
    },
    {
      id: 2,
      type: 'system',
      title: '系统通知',
      content: '平台已更新助医服务流程，请查看学习规范',
      createTime: '2026-05-19 14:00',
      isRead: true
    }
  ]
}

onMounted(() => {
  fetchMessageList()
})
</script>

<style scoped>
.read-message-container {
  max-width: 700px;
  margin: 0 auto;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}
.read-list {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.message-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}
.message-item:hover {
  background-color: #f9f9f9;
}
.message-item:last-child { border-bottom: none; }
.msg-icon { font-size: 32px; }
.msg-content .msg-title { font-size: 18px; font-weight: 600; color: #333; margin-bottom: 6px; }
.msg-content .msg-text { font-size: 16px; color: #666; margin-bottom: 6px; }
.msg-content .msg-time { font-size: 14px; color: #999; }
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
