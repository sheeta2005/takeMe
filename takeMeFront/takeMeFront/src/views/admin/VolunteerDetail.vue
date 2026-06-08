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

    <!-- 基础信息 -->
    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><User /></el-icon>
            <span class="card-title">基础信息</span>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border size="large">
        <el-descriptions-item label="志愿者ID">
          <span class="value-text">{{ volunteerDetail.id }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          <span class="value-text">{{ volunteerDetail.realName }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="账号">
          <span class="value-text">{{ volunteerDetail.username }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          <span class="value-text">{{ volunteerDetail.age }}岁</span>
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          <el-tag :type="volunteerDetail.gender === 0 ? '' : 'danger'" size="small">
            {{ volunteerDetail.gender === 0 ? '男' : '女' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          <span class="value-text">{{ volunteerDetail.phone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">
          <span class="value-text">{{ volunteerDetail.address || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          <span class="time-text">{{ volunteerDetail.createTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="最后登录">
          <span class="time-text">{{ volunteerDetail.lastLoginTime || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 服务信息 -->
    <el-card class="detail-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Briefcase /></el-icon>
            <span class="card-title">服务信息</span>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border size="large">
        <el-descriptions-item label="服务类型">
          <el-tag size="small" :type="getServiceTypeTagType(volunteerDetail.serviceType)">
            {{ getServiceTypeText(volunteerDetail.serviceType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可服务时间">
          <span class="value-text">{{ getServiceDaysText(volunteerDetail.serviceDays) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="工作状态">
          <el-tag :type="getWorkStatusTagType(volunteerDetail.workStatus)" size="small">
            {{ getWorkStatusText(volunteerDetail.workStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="累计服务时长">
          <span class="highlight-value">{{ volunteerDetail.totalServiceHours }}小时</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 紧急联系人 -->
    <el-card class="detail-card emergency-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Phone /></el-icon>
            <span class="card-title">紧急联系人</span>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border size="large">
        <el-descriptions-item label="姓名">
          <span class="value-text">{{ volunteerDetail.emergencyName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="电话">
          <span class="value-text">{{ volunteerDetail.emergencyPhone || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, User, Briefcase, Phone } from '@element-plus/icons-vue'
import { getVolunteerDetail } from '@/api/admin'
import type { Volunteer } from '@/types/Volunteer'

const route = useRoute()
const volunteerDetail = ref<Volunteer>({
  id: 0,
  realName: '',
  username: '',
  phone: '',
  avatar: '',
  serviceDays: 0,
  serviceType: 0,
  workStatus: 0,
  gender: 0,
  age: 20,
  address: '',
  emergencyName: '',
  emergencyPhone: '',
  totalServiceHours: 0,
  status: 1,
  createTime: '',
  lastLoginTime: ''
})

onMounted(() => {
  const volunteerId = Number(route.params.id)
  if (volunteerId) {
    fetchVolunteerDetail(volunteerId)
  }
})

const fetchVolunteerDetail = async (id: number) => {
  try {
    const res = await getVolunteerDetail(id)
    volunteerDetail.value = res.data
  } catch (err) {
    console.error('获取志愿者详情失败', err)
    ElMessage.error('获取志愿者详情失败')
  }
}

const getServiceTypeText = (type: number) => {
  const map = ['代购服务', '助洁服务', '助餐服务', '助医服务', '陪伴服务']
  return map[type] || '未知'
}

const getServiceTypeTagType = (type: number) => {
  const map = ['primary', 'success', 'warning', 'danger', 'info']
  return map[type] || 'info'
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
</script>

<style scoped>
.detail-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
}

.emergency-card {
  max-width: 800px;
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

.value-text {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.time-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.highlight-value {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 16px;
}
</style>
