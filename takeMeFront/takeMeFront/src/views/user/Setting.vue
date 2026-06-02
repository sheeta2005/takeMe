<template>
  <div class="setting-container">
    <div class="setting-card">
      <h2 class="card-title">账号设置</h2>

      <div class="button-group">
        <el-button
          class="action-btn primary-btn"
          @click="goPasswordChange"
        >
          修改密码
        </el-button>

        <el-button
          class="action-btn danger-btn"
          @click="handleLogout"
          :loading="isLoggingOut"
        >
          退出登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isLoggingOut = ref(false)

const goPasswordChange = () => {
  router.push('/user/password-change')
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出当前账号吗？',
      '退出确认',
      {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  isLoggingOut.value = true

  try {
    await userStore.logout()
    ElMessage.success('已安全退出登录')
    await router.replace('/login')
  } catch (err) {
    console.error('退出登录接口异常:', err)
    ElMessage.warning('网络异常，已本地退出')

    userStore.$reset()
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')

    await router.replace('/login')
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<style scoped>
.setting-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  padding: 0;
}

.setting-card {
  width: 100%;
  max-width: 500px;
  background: #fff;
  border-radius: 20px;
  padding: 50px 40px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.08);
  text-align: center;
}

.card-title {
  font-size: 28px;
  font-weight: bold;
  color: #006d5c;
  margin: 0 0 45px 0;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 28px;
  align-items: center;
}

.action-btn {
  width: 320px !important;
  height: 58px !important;
  font-size: 20px !important;
  border-radius: 12px !important;
  border: none !important;
  color: white !important;
  padding: 0 !important;
  margin: 0 !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}

.primary-btn {
  background: #00b899 !important;
}
.primary-btn:hover {
  background: #00a085 !important;
}

.danger-btn {
  background: #f56c6c !important;
}
.danger-btn:hover {
  background: #e64d4d !important;
}
</style>
