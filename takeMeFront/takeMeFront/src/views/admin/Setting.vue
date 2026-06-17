<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
      <p class="page-subtitle">管理系统账号和安全设置</p>
    </div>

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
import { ElMessageBox } from 'element-plus'
import { Warning, SwitchButton } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const adminStore = useAdminStore()

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

    await adminStore.logout()

  } catch (err: any) {
    if (err === 'cancel') {
      return
    }
  }

  window.location.href = '/login'
}
</script>

<style scoped>
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
