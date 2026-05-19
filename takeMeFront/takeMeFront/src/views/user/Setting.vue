<template>
  <div class="setting-container">
    <div class="setting-card">
      <h2 class="card-title">账号设置</h2>

      <div class="button-group">
        <el-button
          class="action-btn primary-btn"
          @click="goFontSize"
        >
          字体大小调节
        </el-button>

        <el-button
          class="action-btn danger-btn"
          @click="handleLogout"
        >
          退出登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/user' // 👈 加上这个

const router = useRouter()
const userStore = useUserStore()

const goFontSize = () => {
  ElMessage.info('字体调节功能开发中')
}

// ✅ 修复：异步 + 调用后端退出接口 + 确保跳转一定执行
const handleLogout = async () => {
  //模拟阶段停止调用
  // try {
  //   await logout() // 调用后端退出
  // } catch (e) {
  //   console.log('退出接口异常，继续本地退出')
  // }

  // 清空状态
  userStore.logout()

  ElMessage.success('已安全退出登录')

  // ✅ 强制跳转，确保一定回到登录页
  await router.replace('/login')
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
