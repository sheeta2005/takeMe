<template>
  <div class="study-container">
    <h2 class="page-title">学习规范</h2>
    <div class="guide-card" v-for="guide in guideList" :key="guide.id">
      <div class="guide-title">{{ guide.title }}</div>
      <div class="guide-content">{{ guide.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const guideList = ref([
  { id: 1, title: '服务基本规范', content: '服务过程中请保持耐心和礼貌，尊重老人的生活习惯和意愿，不与老人发生争执。' },
  { id: 2, title: '助餐服务规范', content: '送餐前请核对餐品信息，确认无误后再送达，提醒老人注意用餐安全。' },
  { id: 3, title: '助洁服务规范', content: '上门服务前请提前联系，服务过程中注意保护老人的个人物品，不随意翻动。' }
])
</script>

<style scoped>
.study-container {
  max-width: 900px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.guide-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  margin-bottom: 16px;
}
.guide-title {
  font-size: 20px;
  font-weight: 600;
  color: #006d5c;
  margin-bottom: 12px;
}
.guide-content {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
}
</style>
