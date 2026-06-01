<template>
  <div class="info-page">
    <h2 class="page-title">个人信息</h2>

    <div class="info-card">
      <div class="avatar-section">
        <img :src="userStore.avatar || defaultAvatar" class="avatar" alt="头像" />
      </div>

      <div class="info-list">
        <div class="info-item">
          <span class="label">姓名：</span>
          <span class="value">{{ userStore.realName || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">账号：</span>
          <span class="value">{{ userStore.account || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号：</span>
          <span class="value">{{ userStore.phone || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">年龄：</span>
          <span class="value">{{ userStore.age || '未填写' }} 岁</span>
        </div>
        <div class="info-item">
          <span class="label">性别：</span>
          <span class="value">{{ userStore.gender === 0 ? '男' : userStore.gender === 1 ? '女' : '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">默认住址：</span>
          <span class="value">{{ defaultAddress || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">紧急联系人：</span>
          <span class="value">
            {{ userStore.emergencyName || '未填写' }} / {{ userStore.emergencyPhone || '未填写' }}
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserAddressList } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 静态资源
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
// 地址列表
const addressList = ref<any[]>([])

// 计算默认地址
const defaultAddress = computed(() => {
  const addr = addressList.value.find(item => item.isDefault === 1)
  return addr?.address || '未填写'
})

// 加载地址列表
const loadAddress = async () => {
  try {
    const res = await getUserAddressList()
    if (res.code === 200) addressList.value = res.data
  } catch (e) {
    ElMessage.error('获取地址失败')
  }
}

// 跳转修改页
const goEdit = () => router.push('/user/info/edit')

onMounted(() => {
  // 从Store获取用户信息 + 接口获取地址
  userStore.getUserInfo()
  loadAddress()
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
