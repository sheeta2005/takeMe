<template>
  <el-container>
    <el-header class="header">
      <div class="logo">takeMe 志愿者端</div>
      <el-button @click="logout">退出</el-button>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/volunteer">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/volunteer/order">
            <el-icon><Document /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/volunteer/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
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
import { House, Document, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>
