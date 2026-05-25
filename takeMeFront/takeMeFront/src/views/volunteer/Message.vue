<template>
  <div class="message-container">
    <h2 class="page-title">消息中心</h2>
    <div class="message-list">
      <div class="message-item" v-for="msg in messageList" :key="msg.id">
        <div class="msg-icon">📩</div>
        <div class="msg-content">
          <div class="msg-title">{{ msg.title }}</div>
          <div class="msg-text">{{ msg.content }}</div>
          <div class="msg-time">{{ msg.time }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const messageList = ref([
  { id: 1, title: '新任务通知', content: '您有一个新的助餐服务任务待确认', time: '2026-05-20 10:30' },
  { id: 2, title: '系统通知', content: '平台已更新助医服务流程，请查看学习规范', time: '2026-05-19 14:00' }
])
</script>

<style scoped>
.message-container {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.message-list {
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
}
.message-item:last-child { border-bottom: none; }
.msg-icon { font-size: 32px; }
.msg-content .msg-title { font-size: 18px; font-weight: 600; color: #333; margin-bottom: 6px; }
.msg-content .msg-text { font-size: 16px; color: #666; margin-bottom: 6px; }
.msg-content .msg-time { font-size: 14px; color: #999; }
</style>
