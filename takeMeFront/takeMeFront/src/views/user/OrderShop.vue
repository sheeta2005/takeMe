<template>
  <div class="service-page">
    <div class="page-title">🛒 代购服务</div>

    <div class="item-list">
      <div class="item" v-for="item in list" :key="item.id">
        <div class="info">
          <div class="name">{{ item.name }}</div>
          <div class="desc">{{ item.desc }}</div>
        </div>
        <div class="price">¥{{ item.price }}</div>
        <el-button type="primary" size="large" @click="goOrder(item)">
          立即下单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const list = ref([
  { id:1, name:'生活用品代购', desc:'米面油、纸巾、清洁用品', price:10 },
  { id:2, name:'药品代购', desc:'非处方常用药品代购', price:15 },
  { id:3, name:'蔬菜生鲜代购', desc:'新鲜蔬菜、水果、肉类', price:12 },
])

const goOrder = (item) => {
  router.push({
    path: '/user/create',
    query: {
      type: '代购',
      desc: item.desc,
      name: item.name,
      price: item.price
    }
  })
}
</script>

<style scoped>
.service-page {
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
.item-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.item {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.info { flex:1; }
.name { font-size:22px; font-weight:600; margin-bottom:6px; }
.desc { font-size:16px; color:#666; }
.price { font-size:20px; color:#f56c6c; font-weight:bold; margin:0 20px; }
</style>
