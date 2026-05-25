<template>
  <div class="info-container">
    <h2 class="page-title">个人信息</h2>
    <div class="info-card">
      <div class="avatar-box">
        <img class="avatar" :src="userStore.avatar" alt="头像" />
      </div>
      <div class="info-list">
        <div class="info-item">
          <span class="label">用户名：</span>
          <span class="value">{{ userStore.username }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号：</span>
          <span class="value">{{ userStore.phone || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务状态：</span>
          <span class="value">正常服务中</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
</script>

<style scoped>
.info-container {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  display: flex;
  align-items: center;
  gap: 32px;
}
.avatar-box .avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}
.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.info-item .label { font-size: 18px; color: #666; width: 100px; display: inline-block; }
.info-item .value { font-size: 18px; color: #333; }
</style>
