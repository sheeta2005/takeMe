<template>
  <div class="info-page">
    <h2 class="page-title">个人信息</h2>

    <div class="info-card">
      <div class="avatar-section">
        <img :src="volunteerStore.avatar || defaultAvatar" class="avatar" alt="头像" />
      </div>

      <div class="info-list">
        <div class="info-item">
          <span class="label">姓名：</span>
          <span class="value">{{ volunteerStore.realName || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">账号：</span>
          <span class="value">{{ volunteerStore.username || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号：</span>
          <span class="value">{{ volunteerStore.phone || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">性别：</span>
          <span class="value">{{ volunteerStore.gender === 0 ? '男' : volunteerStore.gender === 1 ? '女' : '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">年龄：</span>
          <span class="value">{{ volunteerStore.age || '未填写' }} 岁</span>
        </div>
        <div class="info-item">
          <span class="label">居住地址：</span>
          <span class="value">{{ volunteerStore.address || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">可服务时间：</span>
          <span class="value">{{ volunteerStore.serviceDayText }}</span>
        </div>

        <!-- Deleted:<div class="info-item"> -->
        <!-- Deleted:  <span class="label">可服务业务：</span> -->
        <!-- Deleted:  <span class="value">{{ volunteerStore.serviceTypeText }}</span> -->
        <!-- Deleted:</div> -->

        <div class="info-item">
          <span class="label">工作状态：</span>
          <span class="value">{{ volunteerStore.workStatusText }}</span>
        </div>
        <div class="info-item">
          <span class="label">累计服务时长：</span>
          <span class="value">{{ volunteerStore.totalServiceHours || 0 }} 小时</span>
        </div>
        <div class="info-item">
          <span class="label">紧急联系人：</span>
          <span class="value">
            {{ volunteerStore.emergencyName || '未填写' }} / {{ volunteerStore.emergencyPhone || '未填写' }}
          </span>
        </div>
      </div>

      <div class="action-section">
        <el-button type="primary" size="large" @click="goEdit">
          修改信息
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVolunteerStore } from '@/stores/volunteer'

const router = useRouter()
const volunteerStore = useVolunteerStore()

// 静态资源
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 跳转修改页
const goEdit = () => router.push('/volunteer/info/edit')

onMounted(() => {
  // 从Store获取志愿者信息
  volunteerStore.fetchVolunteerInfo()
})
</script>

<style scoped>
.info-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 30px 0;
  text-align: center;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}

.avatar-section {
  text-align: center;
  margin-bottom: 30px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #00b899;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 40px;
}

.info-item {
  display: flex;
  align-items: flex-start;
}

.label {
  font-size: 18px;
  color: #666;
  width: 120px;
  flex-shrink: 0;
}

.value {
  font-size: 18px;
  color: #333;
  word-break: break-all;
}

.action-section {
  text-align: center;
}
</style>
