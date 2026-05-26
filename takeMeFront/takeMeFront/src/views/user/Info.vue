<template>
  <div class="info-page">
    <h2 class="page-title">个人信息</h2>

    <div class="info-card">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <img :src="userInfo.avatar || defaultAvatar" class="avatar" alt="头像" />
      </div>

      <!-- 信息列表 -->
      <div class="info-list">
        <div class="info-item">
          <span class="label">姓名：</span>
          <span class="value">{{ userInfo.username || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">账号：</span>
          <span class="value">{{ userInfo.account || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号：</span>
          <span class="value">{{ userInfo.phone || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">年龄：</span>
          <span class="value">{{ userInfo.age || '未填写' }} 岁</span>
        </div>
        <div class="info-item">
          <span class="label">性别：</span>
          <span class="value">{{ userInfo.gender === 0 ? '男' : '女' }}</span>
        </div>
        <div class="info-item">
          <span class="label">默认住址：</span>
          <span class="value">{{ userInfo.addresses?.[0] || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">紧急联系人：</span>
          <span class="value">
            {{ userInfo.emergencyName || '未填写' }} / {{ userInfo.emergencyPhone || '未填写' }}
          </span>
        </div>
      </div>

      <!-- 修改按钮 -->
      <div class="action-section">
        <el-button type="primary" size="large" @click="goEdit">
          修改信息
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 直接绑定store的state，自动响应式更新
const userInfo = userStore.$state

// 加载用户信息
const loadUserInfo = async () => {
  try {
    await userStore.getUserInfo()
  } catch (e) {
    ElMessage.error('加载用户信息失败')
  }
}

// 跳转到修改页
const goEdit = () => {
  router.push('/user/info/edit')
}

onMounted(() => {
  loadUserInfo()
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
