<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">用户详情</h2>
          <p class="page-subtitle">查看用户详细信息</p>
        </div>
        <el-button size="large" @click="$router.back()">
          <el-icon><Back /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <el-skeleton :rows="8" animated v-if="loading" />

    <template v-else-if="userDetail">
      <el-card class="detail-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon :size="20" color="#00a88d"><User /></el-icon>
              <span class="card-title">基础信息</span>
            </div>
          </div>
        </template>

        <div class="user-header">
          <el-avatar :size="100" :src="userDetail.user?.avatar || defaultAvatar" />
          <div class="user-info">
            <h3 class="user-name">{{ userDetail.user?.realName }}</h3>
            <div class="user-meta">
              <el-tag :type="userDetail.user?.gender === 0 ? '' : 'danger'" size="small">
                {{ userDetail.user?.gender === 0 ? '男' : '女' }}
              </el-tag>
              <span class="meta-text">{{ userDetail.user?.age }}岁</span>
            </div>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">
            <span class="value-text">{{ userDetail.user?.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="账号">
            <span class="value-text">{{ userDetail.user?.username }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            <span class="value-text">{{ userDetail.user?.phone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="居住地址" :span="2">
            <span class="value-text">{{ userDetail.user?.address || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            <span class="time-text">{{ userDetail.user?.createTime }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">
            <span class="time-text">{{ userDetail.user?.lastLoginTime || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="detail-card" shadow="hover" v-if="userDetail.recentOrders && userDetail.recentOrders.length > 0">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon :size="20" color="#00a88d"><ShoppingCart /></el-icon>
              <span class="card-title">最近订单（{{ userDetail.recentOrders.length }}条）</span>
            </div>
          </div>
        </template>

        <el-table :data="userDetail.recentOrders" stripe border style="width: 100%">
          <el-table-column prop="orderNo" label="订单编号" min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusType(row.status)" size="small">
                {{ getOrderStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="订单金额" width="120" align="right">
            <template #default="{ row }">
              <span class="price-text">¥{{ row.totalPrice }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="180" />
        </el-table>
      </el-card>

      <el-card class="detail-card" shadow="hover" v-if="userDetail.reviews && userDetail.reviews.length > 0">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon :size="20" color="#00a88d"><ChatDotRound /></el-icon>
              <span class="card-title">评价记录（{{ userDetail.reviews.length }}条）</span>
            </div>
          </div>
        </template>

        <div class="reviews-list">
          <div v-for="review in userDetail.reviews" :key="review.id" class="review-item">
            <div class="review-header">
              <div class="review-info">
                <el-rate v-model="review.rating" disabled size="small" />
                <span class="review-time">{{ formatTime(review.createTime) }}</span>
              </div>
            </div>
            <div class="review-content" v-if="review.comment">
              {{ review.comment }}
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <el-empty v-else description="用户不存在" :image-size="200" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, User, ShoppingCart, ChatDotRound } from '@element-plus/icons-vue'
import { getUserDetail } from '@/api/admin'

const route = useRoute()
const loading = ref(true)
const userDetail = ref<any>(null)
const defaultAvatar = ref('/佐仓杏子.png')

onMounted(() => {
  const userId = Number(route.params.id)
  if (userId) {
    fetchUserDetail(userId)
  }
})

const fetchUserDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getUserDetail(id)
    if (res.code === 200) {
      userDetail.value = res.data
    }
  } catch (err) {
    console.error('获取用户详情失败', err)
    ElMessage.error('获取用户详情失败')
  } finally {
    loading.value = false
  }
}

const getOrderStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待接单',
    1: '已接单',
    2: '服务中',
    3: '待确认',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知'
}

const getOrderStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'warning',
    4: 'success',
    5: 'danger'
  }
  return map[status] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.detail-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
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
  color: var(--text-primary);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0 0 12px 0;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-text {
  font-size: 14px;
  color: #666;
}

.value-text {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.time-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.price-text {
  color: #f5222d;
  font-weight: bold;
  font-size: 16px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.review-item:hover {
  background: #e8edf3;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.review-time {
  font-size: 12px;
  color: #999;
}

.review-content {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}
</style>
