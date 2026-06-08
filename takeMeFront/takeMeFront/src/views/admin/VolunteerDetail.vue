<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">志愿者详情</h2>
          <p class="page-subtitle">查看志愿者详细信息</p>
        </div>
        <el-button size="large" @click="$router.back()">
          <el-icon><Back /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <el-skeleton :rows="8" animated v-if="loading" />

    <template v-else-if="volunteerDetail">
      <el-card class="detail-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon :size="20" color="#00a88d"><User /></el-icon>
              <span class="card-title">基础信息</span>
            </div>
          </div>
        </template>

        <div class="volunteer-header">
          <el-avatar :size="100" :src="volunteerDetail.volunteer?.avatar || defaultAvatar" />
          <div class="volunteer-info">
            <h3 class="volunteer-name">{{ volunteerDetail.volunteer?.realName }}</h3>
            <div class="volunteer-stats">
              <el-tag type="success" size="large">
                完成服务 {{ volunteerDetail.completedCount }} 次
              </el-tag>
              <el-tag type="warning" size="large">
                平均评分 {{ volunteerDetail.averageRating }} 分
              </el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="志愿者ID">
            <span class="value-text">{{ volunteerDetail.volunteer?.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="账号">
            <span class="value-text">{{ volunteerDetail.volunteer?.username }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            <span class="value-text">{{ volunteerDetail.volunteer?.phone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            <el-tag :type="volunteerDetail.volunteer?.gender === 0 ? '' : 'danger'" size="small">
              {{ volunteerDetail.volunteer?.gender === 0 ? '男' : '女' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            <span class="value-text">{{ volunteerDetail.volunteer?.age || '-' }}岁</span>
          </el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">
            <span class="value-text">{{ volunteerDetail.volunteer?.address || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="可服务时间">
            <span class="value-text">{{ getServiceDaysText(volunteerDetail.volunteer?.serviceDays) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="工作状态">
            <el-tag :type="getWorkStatusTagType(volunteerDetail.volunteer?.workStatus)" size="small">
              {{ getWorkStatusText(volunteerDetail.volunteer?.workStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            <span class="time-text">{{ volunteerDetail.volunteer?.createTime }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">
            <span class="time-text">{{ volunteerDetail.volunteer?.lastLoginTime || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="detail-card" shadow="hover" v-if="volunteerDetail.reviews && volunteerDetail.reviews.length > 0">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon :size="20" color="#00a88d"><ChatDotRound /></el-icon>
              <span class="card-title">用户评价（{{ volunteerDetail.reviews.length }}条）</span>
            </div>
          </div>
        </template>

        <div class="reviews-list">
          <div v-for="review in volunteerDetail.reviews" :key="review.id" class="review-item">
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

    <el-empty v-else description="志愿者不存在" :image-size="200" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, User, ChatDotRound } from '@element-plus/icons-vue'
import { getVolunteerDetail } from '@/api/admin'

const route = useRoute()
const loading = ref(true)
const volunteerDetail = ref<any>(null)
const defaultAvatar = ref('/default-avatar.png')

onMounted(() => {
  const volunteerId = Number(route.params.id)
  if (volunteerId) {
    fetchVolunteerDetail(volunteerId)
  }
})

const fetchVolunteerDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getVolunteerDetail(id)
    if (res.code === 200) {
      volunteerDetail.value = res.data
    }
  } catch (err) {
    console.error('获取志愿者详情失败', err)
    ElMessage.error('获取志愿者详情失败')
  } finally {
    loading.value = false
  }
}

const getServiceDaysText = (days: string | number) => {
  const dayMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  if (typeof days === 'string') {
    return days.split(',').map(d => dayMap[parseInt(d)]).join('、')
  }
  return dayMap[days] || '未知'
}

const getWorkStatusText = (status: number) => {
  const map = ['休息中', '待命中', '服务中']
  return map[status] || '未知'
}

const getWorkStatusTagType = (status: number) => {
  const map = ['info', 'primary', 'success']
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

.volunteer-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
}

.volunteer-info {
  flex: 1;
}

.volunteer-name {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0 0 12px 0;
}

.volunteer-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
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
