<template>
  <div class="detail-container">
    <div class="header-row">
      <h2 class="page-title">用户详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <div class="detail-card">
      <div class="detail-section">
        <h3 class="section-title">基础信息</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">用户ID：</span>
            <span class="value">{{ userDetail.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">姓名：</span>
            <span class="value">{{ userDetail.realName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">账号：</span>
            <span class="value">{{ userDetail.username }}</span>
          </div>
          <div class="detail-item">
            <span class="label">年龄：</span>
            <span class="value">{{ userDetail.age }}岁</span>
          </div>
          <div class="detail-item">
            <span class="label">性别：</span>
            <el-tag :type="userDetail.gender === 0 ? 'primary' : 'danger'" size="small">
              {{ userDetail.gender === 0 ? '男' : '女' }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="label">手机号：</span>
            <span class="value">{{ userDetail.phone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">居住地址：</span>
            <span class="value">{{ userDetail.address || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">注册时间：</span>
            <span class="value">{{ userDetail.createTime }}</span>
          </div>
          <div class="detail-item">
            <span class="label">最后登录：</span>
            <span class="value">{{ userDetail.lastLoginTime || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">紧急联系人</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">姓名：</span>
            <span class="value">{{ userDetail.emergencyName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">联系电话：</span>
            <span class="value">{{ userDetail.emergencyPhone || '-' }}</span>
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
