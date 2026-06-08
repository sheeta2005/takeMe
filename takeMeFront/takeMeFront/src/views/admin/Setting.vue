<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
      <p class="page-subtitle">管理系统账号和安全设置</p>
    </div>

    <!-- 账号信息卡片 -->
    <el-card class="setting-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><User /></el-icon>
            <span class="card-title">账号信息</span>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border size="large">
        <el-descriptions-item label="管理员ID">
          <span class="value-text">{{ adminInfo.id || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          <span class="value-text">{{ adminInfo.username || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="真实姓名">
          <span class="value-text">{{ adminInfo.realName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="最后登录">
          <span class="time-text">{{ adminInfo.lastLoginTime || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 安全操作卡片 -->
    <el-card class="action-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#ef4444"><Warning /></el-icon>
            <span class="card-title">安全操作</span>
          </div>
        </div>
      </template>

      <div class="action-content">
        <div class="action-item">
          <div class="action-info">
            <div class="action-title">退出登录</div>
            <div class="action-desc">退出当前账号，需要重新登录才能继续使用</div>
          </div>
          <el-button type="danger" size="large" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Warning, SwitchButton } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'
import { adminLogout } from '@/api/admin'

const adminStore = useAdminStore()
const adminInfo = ref({
  id: '',
  username: '',
  realName: '',
  lastLoginTime: ''
})

onMounted(() => {
  // 从 store 获取管理员信息
  adminInfo.value = {
    id: adminStore.admin?.id || '',
    username: adminStore.admin?.username || '',
    realName: adminStore.admin?.realName || '',
    lastLoginTime: adminStore.admin?.lastLoginTime || ''
  }
})

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
.setting-card,
.action-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
  max-width: 900px;
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

.action-content {
  padding: 10px 0;
}

.action-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.action-info {
  flex: 1;
}

.action-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.action-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
