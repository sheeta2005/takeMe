<template>
  <div class="msg-detail-container">
    <div class="header">
      <el-button type="text" @click="back">← 返回</el-button>
      <h2>消息详情</h2>
    </div>

    <div class="detail-card" v-if="msg">
      <div class="title">{{ msg.title }}</div>
      <div class="time">{{ msg.createTime }}</div>
      <div class="content">
        <!-- 渲染内容，订单号高亮可点击 -->
        <span v-if="msg.relatedId">
          {{ msg.content.replace(msg.relatedId, '') }}
          <span
            class="order-no"
            @click="toOrder"
          >
            {{ msg.relatedId }}
          </span>
        </span>
        <span v-else>{{ msg.content }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { MessageInfo } from '@/types/message'

const router = useRouter()
const route = useRoute()
const msg = ref<MessageInfo | null>(null)

const messageList = ref<MessageInfo[]>([
  {
    id: 1,
    type: 'task',
    title: '新任务通知',
    content: '您有一个新的助餐服务任务，订单号：ORD20260520001，请及时处理',
    createTime: '2026-05-20 10:30',
    isRead: false,
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
])

onMounted(() => {
  const id = Number(route.params.id)
  msg.value = messageList.value.find(item => item.id === id) || null

  // 进入详情页 → 自动标记已读
  if (msg.value && !msg.value.isRead) {
    msg.value.isRead = true
  }
})

// 返回
const back = () => router.go(-1)

// 跳转到订单详情
const toOrder = () => {
  if (msg.value?.relatedUrl) {
    router.push(msg.value.relatedUrl)
  }
}
</script>

<style scoped>
.msg-detail-container {
  max-width: 700px;
  margin: 0 auto;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.detail-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 8px;
}
.time {
  color: #999;
  font-size: 14px;
  margin-bottom: 24px;
}
.content {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
}
/* 订单号高亮可点击 */
.order-no {
  color: #00b899;
  font-weight: bold;
  cursor: pointer;
  text-decoration: underline;
  margin-left: 4px;
}
</style>
