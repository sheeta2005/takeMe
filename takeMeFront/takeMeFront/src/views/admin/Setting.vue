<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
      <p class="page-subtitle">管理系统账号和安全设置</p>
    </div>

    <el-card class="profile-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#409EFF"><User /></el-icon>
            <span class="card-title">个人信息</span>
          </div>
        </div>
      </template>

      <div class="profile-content">
        <div class="avatar-section">
          <Avatar :src="adminStore.avatar" :size="100" />
          <div class="avatar-actions">
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforeUpload"
              :http-request="handleAvatarUpload"
            >
              <el-button type="primary" size="small">更换头像</el-button>
            </el-upload>
            <div class="upload-tip">支持 jpg/png/webp，最大 5MB</div>
          </div>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="姓名">
            {{ adminStore.realName || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="账号">
            {{ adminStore.username || '未绑定' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

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
import { ElMessageBox, ElMessage } from 'element-plus'
import { Warning, SwitchButton, User } from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'
import { uploadAvatar } from '@/api/admin'
import { compressImage } from '@/utils/imageCompress'
import Avatar from '@/components/Avatar.vue'

const adminStore = useAdminStore()

const handleBeforeUpload = (file: File) => {
  const isValidType = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'].includes(file.type)
  const isValidSize = file.size / 1024 / 1024 < 5

  if (!isValidType) {
    ElMessage.error('只支持 JPG/PNG/WEBP 格式')
    return false
  }
  if (!isValidSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options: any) => {
  const { file } = options

  try {
    ElMessage.info('正在压缩图片...')
    const compressedFile = await compressImage(file, 800, 0.8)

    ElMessage.info('正在上传...')
    const res = await uploadAvatar(compressedFile)

    if (res.code === 200) {
      adminStore.avatar = res.data
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (err: any) {
    console.error('上传失败:', err)
    ElMessage.error(err.message || '上传失败')
  }
}

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
.profile-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
  max-width: 900px;
}

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

.profile-content {
  padding: 20px 0;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 30px;
  margin-bottom: 30px;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
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
