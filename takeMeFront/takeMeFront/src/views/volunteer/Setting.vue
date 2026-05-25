<template>
  <div class="setting-container">
    <h2 class="page-title">账号设置</h2>
    <div class="setting-card">
      <div class="setting-item">
        <span class="label">修改密码</span>
        <el-button type="primary">修改</el-button>
      </div>
      <div class="setting-item">
        <span class="label">绑定手机号</span>
        <span class="value">{{ userStore.phone || '未绑定' }}</span>
        <el-button type="primary" v-if="!userStore.phone">绑定</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
</script>

<style scoped>
.setting-container {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.setting-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
}
.setting-item:last-child { border-bottom: none; }
.label { font-size: 18px; color: #333; }
.value { font-size: 18px; color: #666; }
</style>
