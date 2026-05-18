<template>
  <div class="setting-container">
    <div class="title">设置</div>

    <div class="btn-list">
      <el-button type="primary" size="large" @click="goEdit" class="big-btn">
        修改老人信息
      </el-button>

      <el-button type="danger" size="large" @click="logout" class="big-btn">
        退出登录
      </el-button>

      <el-button size="large" @click="goHome" class="big-btn">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 修改信息
const goEdit = () => {
  router.push('/user/info/edit').catch(err => {
    console.error('跳转编辑页失败', err)
    ElMessage.error('跳转失败')
  })
}

// 退出登录
const logout = async () => {
  try {
    await ElMessageBox.confirm('确定退出当前账号？', '提示', {
      type: 'warning'
    })
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (e) {
    console.error('退出失败', e)
  }
}

// 返回首页
const goHome = () => {
  router.push('/user/index').catch(err => {
    console.error('返回首页失败', err)
    ElMessage.error('返回失败')
  })
}
</script>

<style scoped>
.setting-container {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 40px 24px;
  box-sizing: border-box;
}
.title {
  font-size: 32px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 60px;
  color: #333;
}
.btn-list {
  display: flex;
  flex-direction: column;
  gap: 30px;
  max-width: 400px;
  margin: 0 auto;
}
.big-btn {
  width: 100%;
  height: 60px;
  font-size: 22px;
  border-radius: 12px;
}
</style>
