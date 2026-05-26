<template>
  <div class="page-container">
    <h2 class="page-title">收信箱</h2>
    <el-list :data="msgList">
      <el-list-item v-for="msg in msgList" :key="msg.id">
        <div>{{ msg.title }}</div>
        <div style="color:#999; font-size:14px;">{{ msg.time }}</div>
      </el-list-item>
    </el-list>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const msgList = ref([
  { id: 1, title: '平台更新通知', time: '2026-05-25' }
])
</script>

<style scoped>
.page-container { max-width: 1200px; }
.page-title { font-size: 28px; font-weight: bold; color: #333; margin-bottom: 24px; }
</style>
