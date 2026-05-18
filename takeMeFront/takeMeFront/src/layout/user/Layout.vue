<template>
  <el-container>
    <el-header class="header">
      <div class="logo">takeMe 老人端</div>
      <el-button @click="logout">退出</el-button>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header {
  background: #ffc107;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  color: #fff;
}
.main { background: #f5f5f5; padding: 20px; }
</style>
