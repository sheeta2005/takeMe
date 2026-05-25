<template>
  <div class="home-container">
    <!-- 问候卡片 -->
    <div class="greeting-card">
      <div class="greeting-icon">🤝</div>
      <div class="greeting-text">
        <h2>您好，{{ userName }} 志愿者</h2>
        <p>感谢您加入助老服务平台，您的每一份付出，都是老人温暖的依靠</p>
      </div>
    </div>

    <!-- 平台公告 -->
    <div class="news-card">
      <h3 class="card-title">📢 志愿者公告</h3>
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

onMounted(() => {
  userName.value = userStore.username || '尊敬的志愿者'
})

const newsList = ref([
  { title: '助餐服务：每日10点前接单，优先配送', date: '2025-01-01' },
  { title: '助洁服务：请按预约时间上门，提前10分钟联系老人', date: '2025-01-02' },
  { title: '助医服务：陪同就诊请提前熟悉医院流程，协助老人挂号', date: '2025-01-03' },
  { title: '代购服务：请核对药品/生活用品清单，确保无遗漏', date: '2025-01-04' },
])
</script>

<style scoped>
.home-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}

.greeting-card {
  background: linear-gradient(135deg, #ffffff 0%, #f7fffd 100%);
  border-radius: 20px;
  padding: 32px;
  display: flex;
  align-items: center;
  gap: 24px;
  box-shadow: 0 8px 24px rgba(0, 184, 153, 0.12);
  margin-bottom: 30px;
  border: 1px solid rgba(0, 184, 153, 0.1);
  transition: all 0.3s ease;
}
.greeting-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(0, 184, 153, 0.15);
}

.greeting-icon {
  font-size: 56px;
}

.greeting-text h2 {
  font-size: 28px;
  color: #006d5c;
  margin: 0 0 8px 0;
  font-weight: 700;
}

.greeting-text p {
  font-size: 18px;
  color: #555;
  margin: 0;
  line-height: 1.6;
}

.news-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(0, 184, 153, 0.08);
  border: 1px solid rgba(0, 184, 153, 0.07);
}

.card-title {
  font-size: 24px;
  color: #006d5c;
  margin: 0 0 24px 0;
  font-weight: 700;
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
  padding: 16px 20px;
  border-radius: 12px;
  background: #fafcfb;
  transition: all 0.2s ease;
}
.news-item:hover {
  background: #f2faf8;
}

.news-title {
  font-size: 18px;
  color: #222;
  font-weight: 500;
}

.news-date {
  font-size: 15px;
  color: #888;
}
</style>
