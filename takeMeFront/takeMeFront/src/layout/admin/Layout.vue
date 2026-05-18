<template>
  <el-container>
    <el-header class="header">
      <div class="logo">takeMe 公益服务平台</div>
      <div class="header-right">
        <el-button @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/admin">
            <el-icon><House /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/admin/order">
            <el-icon><Document /></el-icon>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/volunteer">
            <el-icon><User /></el-icon>
            <span>志愿者管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/user">
            <el-icon><UserFilled /></el-icon>
            <span>老人管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { House, Document, User, UserFilled } from '@element-plus/icons-vue'
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
}
.logo { color: #fff; font-size: 20px; font-weight: bold; }
.aside { background: #f5f5f5; }
.main { background: #f0f2f5; }
</style>
