<template>
  <div class="setting-container">
    <div class="setting-card">
      <h2 class="card-title">账号设置</h2>

      <div class="button-group">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { adminLogout } from '@/api/admin'

const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出当前账号吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await adminLogout()
      const adminStore = useAdminStore()
      adminStore.logout()

      ElMessage.success('已安全退出登录')
    } catch (err) {
      console.error('退出接口异常，继续本地退出', err)
      ElMessage.warning('网络异常，已本地退出')
    } finally {
      window.location.href = '/login'
    }
  })
}
</script>

<style scoped>
.setting-container {
  width: 100%;
  height: calc(100vh - 64px);
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f8f9fa;
  margin: 0;
  padding: 0;
}

.setting-card {
  width: 100%;
  max-width: 450px;
  background: #fff;
  border-radius: 20px;
  padding: 60px 40px;
  box-shadow: 0 4px 20px rgba(0, 184, 153, 0.08);
  text-align: center;
}

.card-title {
  font-size: 28px;
  font-weight: bold;
  color: #006d5c;
  margin: 0 0 50px 0;
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
  cursor: pointer;
  transition: all 0.2s ease;
}

.danger-btn {
  background: #f56c6c !important;
}
.danger-btn:hover {
  background: #e64d4d !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
}
</style>
