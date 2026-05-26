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
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const goFontSize = () => {
  ElMessage.info('字体调节功能开发中')
}

// 退出登录：确认 → 清空 → 跳转到主页 /
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 模拟阶段先注释，后期打开
    // await logout()

    // 清空用户状态
    userStore.logout()

    ElMessage.success('已安全退出')

    // 跳转到主页（不是登录页）
    const navigationResult = await router.push('/')
    // 防止守卫拦截导致没跳走
    if (navigationResult && navigationResult.type === 'failure') {
      console.warn('跳转被拦截，尝试 replace')
      await router.replace('/')
    }
  } catch (err: any) {
    // 取消或报错不处理
    if (err !== 'cancel') {
      console.error('退出异常：', err)
    }
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
