<template>
  <div class="detail-container">
    <div class="header-row">
      <h2 class="page-title">志愿者详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <div class="detail-card">
      <div class="detail-section">
        <h3 class="section-title">基础信息</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">志愿者ID：</span>
            <span class="value">{{ volunteerDetail.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">姓名：</span>
            <span class="value">{{ volunteerDetail.realName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">账号：</span>
            <span class="value">{{ volunteerDetail.username }}</span>
          </div>
          <div class="detail-item">
            <span class="label">年龄：</span>
            <span class="value">{{ volunteerDetail.age }}岁</span>
          </div>
          <div class="detail-item">
            <span class="label">性别：</span>
            <el-tag :type="volunteerDetail.gender === 0 ? 'primary' : 'danger'" size="small">
              {{ volunteerDetail.gender === 0 ? '男' : '女' }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">手机号：</span>
            <span class="value">{{ volunteerDetail.phone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">地址：</span>
            <span class="value">{{ volunteerDetail.address || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">注册时间：</span>
            <span class="value">{{ volunteerDetail.createTime }}</span>
          </div>
          <div class="detail-item">
            <span class="label">最后登录：</span>
            <span class="value">{{ volunteerDetail.lastLoginTime || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">服务信息</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">服务类型：</span>
            <el-tag size="small" :type="getServiceTypeTagType(volunteerDetail.serviceType)">
              {{ getServiceTypeText(volunteerDetail.serviceType) }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">可服务时间：</span>
            <span class="value">{{ getServiceDaysText(volunteerDetail.serviceDays) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">工作状态：</span>
            <el-tag :type="getWorkStatusTagType(volunteerDetail.workStatus)" size="small">
              {{ getWorkStatusText(volunteerDetail.workStatus) }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">累计服务时长：</span>
            <span class="value" style="color: #00a88d; font-weight: 600">{{ volunteerDetail.totalServiceHours }}小时</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">紧急联系人</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">姓名：</span>
            <span class="value">{{ volunteerDetail.emergencyName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">电话：</span>
            <span class="value">{{ volunteerDetail.emergencyPhone || '-' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const getServiceDaysText = (days: number) => {
  const map = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return map[days] || '未知'
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
.detail-container {
  width: 100%;
  padding: 10px 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 30px;
}

.detail-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.label {
  width: 120px;
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.value {
  font-size: 14px;
  color: #333;
}
</style>
