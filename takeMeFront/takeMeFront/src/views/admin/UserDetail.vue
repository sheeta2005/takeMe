<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">用户详情</h2>
          <p class="page-subtitle">查看老人用户详细信息</p>
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
        <el-descriptions-item label="用户ID">
          <span class="value-text">{{ userDetail.id }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          <span class="value-text">{{ userDetail.realName }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="账号">
          <span class="value-text">{{ userDetail.username }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          <span class="value-text">{{ userDetail.age }}岁</span>
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          <el-tag :type="userDetail.gender === 0 ? '' : 'danger'" size="small">
            {{ userDetail.gender === 0 ? '男' : '女' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          <span class="value-text">{{ userDetail.phone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="居住地址" :span="2">
          <span class="value-text">{{ userDetail.address || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          <span class="time-text">{{ userDetail.createTime }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="最后登录">
          <span class="time-text">{{ userDetail.lastLoginTime || '-' }}</span>
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
          <span class="value-text">{{ userDetail.emergencyName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          <span class="value-text">{{ userDetail.emergencyPhone || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, User, Phone } from '@element-plus/icons-vue'
import { getUserDetail } from '@/api/admin'
import type { User } from '@/types/User'

const route = useRoute()
const userDetail = ref<User>({
  id: 0,
  realName: '',
  username: '',
  phone: '',
  avatar: '',
  gender: 0,
  age: 60,
  address: '',
  emergencyName: '',
  emergencyPhone: '',
  status: 1,
  createTime: '',
  lastLoginTime: ''
})

onMounted(() => {
  const userId = Number(route.params.id)
  if (userId) {
    fetchUserDetail(userId)
  }
})

const fetchUserDetail = async (id: number) => {
  try {
    const res = await getUserDetail(id)
    userDetail.value = res.data
  } catch (err) {
    console.error('获取用户详情失败', err)
    ElMessage.error('获取用户详情失败')
  }
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
</style>
