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
import { useVolunteerStore } from '@/stores/volunteer'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const volunteerStore = useVolunteerStore()

const goFontSize = () => {
  ElMessage.info('字体调节功能开发中')
}

// ✅ 最终修复版：所有逻辑统一交给store，页面只负责确认和跳转
const handleLogout = async () => {
  try {
    // 1. 只做一件事：弹出确认框
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 2. 只调用一次store的logout，所有接口、状态、提示都在store里处理
    await volunteerStore.logout()

  } catch (err: any) {
    // 只有用户点击"取消"才会走到这里
    if (err === 'cancel') {
      return
    }
    // 接口错误已经在store里处理了，这里不用管
  }

  // ✅ 关键：跳转逻辑放在最外面，无论接口成功失败一定会执行
  // 用window.location.href强制跳转，彻底避免路由守卫拦截
  window.location.href = '/login'
}
</script>

<style scoped>
/* 所有样式完全不变，和用户端保持一致 */
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
