<template>
  <div class="home-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="hover">
      <div class="banner-content">
        <div class="banner-left">
          <el-avatar :size="72" :src="userStore.avatar || defaultAvatar" class="user-avatar">
            <el-icon :size="36"><UserFilled /></el-icon>
          </el-avatar>
          <div class="banner-text">
            <h2 class="banner-title">您好，{{ userName }} 老人 👋</h2>
            <p class="banner-subtitle">欢迎使用助老服务平台，我们将竭诚为您提供便捷、安心的服务</p>
          </div>
        </div>
        <div class="banner-stats">
          <el-statistic title="我的订单" :value="orderCount">
            <template #prefix>
              <el-icon color="#00a88d"><Document /></el-icon>
            </template>
          </el-statistic>
        </div>
      </div>
    </el-card>

    <!-- 快捷服务入口 -->
    <el-card class="service-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Grid /></el-icon>
            <span class="card-title">快捷服务</span>
          </div>
        </div>
      </template>
      <div class="service-grid">
        <div class="service-item" @click="router.push('/user/meal')">
          <div class="service-icon meal-icon">
            <el-icon :size="36"><Dish /></el-icon>
          </div>
          <div class="service-name">助餐服务</div>
          <div class="service-desc">营养配送</div>
        </div>
        <div class="service-item" @click="router.push('/user/clean')">
          <div class="service-icon clean-icon">
            <el-icon :size="36"><Brush /></el-icon>
          </div>
          <div class="service-name">助洁服务</div>
          <div class="service-desc">居家清洁</div>
        </div>
        <div class="service-item" @click="router.push('/user/medical')">
          <div class="service-icon medical-icon">
            <el-icon :size="36"><FirstAidKit /></el-icon>
          </div>
          <div class="service-name">助医服务</div>
          <div class="service-desc">陪同就医</div>
        </div>
        <div class="service-item" @click="router.push('/user/shop')">
          <div class="service-icon shop-icon">
            <el-icon :size="36"><ShoppingBag /></el-icon>
          </div>
          <div class="service-name">代购服务</div>
          <div class="service-desc">生活用品</div>
        </div>
        <div class="service-item" @click="router.push('/user/companion')">
          <div class="service-icon companion-icon">
            <el-icon :size="36"><ChatLineRound /></el-icon>
          </div>
          <div class="service-name">陪伴服务</div>
          <div class="service-desc">精神慰藉</div>
        </div>
      </div>
    </el-card>

    <!-- 平台公告 -->
    <el-row :gutter="24">
      <el-col :xs="24" :lg="16">
        <el-card class="news-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#f59e0b"><Bell /></el-icon>
                <span class="card-title">平台公告</span>
              </div>
            </div>
          </template>
          <div v-if="newsList.length > 0" class="news-list">
            <div class="news-item" v-for="(item, i) in newsList" :key="i">
              <div class="news-left">
                <el-icon :size="20" color="#00a88d"><Document /></el-icon>
                <span class="news-title">{{ item.title }}</span>
              </div>
              <el-tag size="small" type="info">{{ item.date }}</el-tag>
            </div>
          </div>
          <div v-else class="empty-tip">
            <el-empty description="暂无公告" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="tips-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="20" color="#3b82f6"><InfoFilled /></el-icon>
                <span class="card-title">温馨提示</span>
              </div>
            </div>
          </template>
          <el-alert
            v-for="(tip, index) in tipsList"
            :key="index"
            :title="tip"
            type="success"
            :closable="false"
            show-icon
            class="tip-item"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 服务数据统计 -->
    <el-card class="stats-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><DataAnalysis /></el-icon>
            <span class="card-title">我的服务统计</span>
          </div>
        </div>
      </template>
      <div class="stats-grid">
        <div class="stat-item">
          <el-icon :size="32" color="#00a88d"><ShoppingCart /></el-icon>
          <div class="stat-value">{{ serviceStats.total }}</div>
          <div class="stat-label">总服务次数</div>
        </div>
        <div class="stat-item">
          <el-icon :size="32" color="#10b981"><CircleCheck /></el-icon>
          <div class="stat-value">{{ serviceStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <div class="stat-item">
          <el-icon :size="32" color="#f59e0b"><Clock /></el-icon>
          <div class="stat-value">{{ serviceStats.pending }}</div>
          <div class="stat-label">进行中</div>
        </div>
        <div class="stat-item">
          <el-icon :size="32" color="#3b82f6"><Star /></el-icon>
          <div class="stat-value">{{ serviceStats.rating }}</div>
          <div class="stat-label">平均评分</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserStatistics } from '@/api/user'
import { getMyOrderList } from '@/api/order'
import {
  UserFilled, Document, Grid, Dish, Brush, FirstAidKit,
  ShoppingBag, ChatLineRound, Bell, InfoFilled, DataAnalysis,
  ShoppingCart, CircleCheck, Clock, Star
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userName = ref('')
const orderCount = ref(0)
const serviceStats = ref({
  total: 0,
  completed: 0,
  pending: 0,
  rating: '0.0'
})

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const tipsList = [
  '请提前10分钟准备好服务所需物品',
  '服务完成后请及时评价',
  '遇到问题可联系客服',
  '定期更新个人信息以便我们更好地为您服务'
]

const newsList = [
  { title: '助餐服务每日10点前下单，当日配送', date: '2025-01-01' },
  { title: '助洁服务支持预约上门时间', date: '2025-01-02' },
  { title: '助医服务可协助挂号、陪同就诊', date: '2025-01-03' },
  { title: '代购服务支持生活用品、药品代买', date: '2025-01-04' },
]

onMounted(async () => {
  // 先加载用户基本信息
  await userStore.getUserInfo()
  userName.value = userStore.realName || userStore.username || '尊敬的用户'

  try {
    // 获取用户统计数据
    const statsRes = await getUserStatistics()
    if (statsRes.data) {
      serviceStats.value = statsRes.data
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }

  try {
    // 获取订单总数
    const orderRes = await getMyOrderList({ pageNum: 1, pageSize: 1 })
    if (orderRes.data) {
      orderCount.value = orderRes.data.total || 0
    }
  } catch (e) {
    console.error('获取订单数失败', e)
  }
})
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 0;
}

/* 欢迎横幅 */
.welcome-banner {
  margin-bottom: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f0fffc 100%);
  border: 1px solid rgba(0, 168, 141, 0.1);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  border: 3px solid rgba(0, 168, 141, 0.2);
  box-shadow: 0 4px 12px rgba(0, 168, 141, 0.15);
}

.banner-title {
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.banner-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* 快捷服务 */
.service-card {
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.service-item:hover {
  background: #f8faf9;
  transform: translateY(-4px);
}

.service-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.meal-icon { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); }
.clean-icon { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); }
.medical-icon { background: linear-gradient(135deg, #10b981 0%, #34d399 100%); }
.shop-icon { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); }
.companion-icon { background: linear-gradient(135deg, #ef4444 0%, #f87171 100%); }

.service-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.service-desc {
  font-size: 12px;
  color: #64748b;
}

/* 公告和提示 */
.news-card,
.tips-card {
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f8faf9;
  transition: all 0.2s ease;
}

.news-item:hover {
  background: #f0fffc;
}

.news-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.news-title {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.tip-item {
  margin-bottom: 12px;
  border-radius: 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

/* 统计卡片 */
.stats-card {
  border: 1px solid #e2e8f0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-radius: 12px;
  background: #f8faf9;
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: #f0fffc;
  transform: translateY(-2px);
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1e293b;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

@media (max-width: 768px) {
  .service-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .banner-content {
    flex-direction: column;
    text-align: center;
  }
}

.empty-tip {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
