<template>
  <div class="meal-page">
    <div class="page-title">🍚 助餐服务</div>

    <div class="meal-list">
      <div class="meal-item" v-for="item in mealList" :key="item.id">
        <div class="meal-info">
          <div class="meal-name">{{ item.name }}</div>
          <div class="meal-desc">{{ item.desc }}</div>
        </div>
        <div class="meal-price">¥{{ item.price }}</div>
        <el-button type="primary" size="large" @click="goToOrder(item)">
          立即下单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 餐品列表
const mealList = ref([
  { id: 1, name: '营养套餐A', desc: '一荤两素 适合日常', price: 15 },
  { id: 2, name: '营养套餐B', desc: '两荤一素 营养均衡', price: 18 },
  { id: 3, name: '软食易消化餐', desc: '软烂易咀嚼 适合牙口不佳', price: 20 },
  { id: 4, name: '清淡养生餐', desc: '低盐低脂 健康养生', price: 22 },
])

// 跳转到下单页面
const goToOrder = (item) => {
  router.push({
    path: '/user/create',
    query: {
      type: '助餐',
      desc: item.desc,
      name: item.name,
      price: item.price
    }
  })
}
</script>

<style scoped>
.meal-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #006d5c;
  text-align: center;
  margin-bottom: 40px;
}

.meal-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.meal-item {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}

.meal-info {
  flex: 1;
}

.meal-name {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.meal-desc {
  font-size: 16px;
  color: #666;
}

.meal-price {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
  margin: 0 20px;
}
</style>
