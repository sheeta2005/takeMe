<template>
  <div class="home-container">
    <!-- 问候卡片 -->
    <div class="greeting-card">
      <div class="greeting-icon">👋</div>
      <div class="greeting-text">
        <h2>您好，{{ userName }} 老人</h2>
        <p>欢迎使用助老服务平台，我们将竭诚为您提供便捷、安心的服务</p>
      </div>
    </div>

    <!-- 新闻公告 -->
    <div class="news-card">
      <h3 class="card-title">📢 平台公告</h3>
      <div class="news-list">
        <div class="news-item" v-for="(item, i) in newsList" :key="i">
          <span class="news-title">{{ item.title }}</span>
          <span class="news-date">{{ item.date }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const userName = ref('')

// 从 userStore 获取用户名，没有就用默认值
onMounted(() => {
  userName.value = userStore.username || '尊敬的用户'
})

// 公告数据
const newsList = ref([
  { title: '助餐服务每日10点前下单，当日配送', date: '2025-01-01' },
  { title: '助洁服务支持预约上门时间', date: '2025-01-02' },
  { title: '助医服务可协助挂号、陪同就诊', date: '2025-01-03' },
  { title: '代购服务支持生活用品、药品代买', date: '2025-01-04' },
])
</script>

<style scoped>
.home-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}

.greeting-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px;
  display: flex;
  align-items: center;
  gap: 24px;
  box-shadow: 0 4px 20px rgba(0, 184, 153, 0.08);
  margin-bottom: 30px;
}

.greeting-icon {
  font-size: 56px;
}

.greeting-text h2 {
  font-size: 28px;
  color: #006d5c;
  margin: 0 0 8px 0;
}

.greeting-text p {
  font-size: 18px;
  color: #666;
  margin: 0;
}

.news-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 184, 153, 0.08);
}

.card-title {
  font-size: 24px;
  color: #006d5c;
  margin: 0 0 24px 0;
  font-weight: bold;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.news-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.news-item:last-child {
  border-bottom: none;
}

.news-title {
  font-size: 18px;
  color: #333;
}

.news-date {
  font-size: 15px;
  color: #999;
}
</style>
