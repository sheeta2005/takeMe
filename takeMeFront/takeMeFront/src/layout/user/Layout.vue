<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">takeMe 老人服务平台</div>
      <div class="header-right">
        <div class="user-box">
          <img class="user-avatar" :src="userStore.avatar" alt="头像" />
          <span>您好，{{ userName }} 老人</span>
        </div>
      </div>
    </el-header>

    <el-container>
      <el-aside width="240px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          class="aside-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/user">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-menu-item index="/user/meal">
            <el-icon><Dish /></el-icon>
            <span>助餐服务</span>
          </el-menu-item>

          <el-menu-item index="/user/clean">
            <el-icon><Brush /></el-icon>
            <span>助洁服务</span>
          </el-menu-item>

          <el-menu-item index="/user/medical">
            <el-icon><FirstAidKit /></el-icon>
            <span>助医服务</span>
          </el-menu-item>

          <el-menu-item index="/user/shop">
            <el-icon><ShoppingCart /></el-icon>
            <span>代购服务</span>
          </el-menu-item>

          <el-menu-item index="/user/order">
            <el-icon><Tickets /></el-icon>
            <span>我的订单</span>
          </el-menu-item>

          <el-menu-item index="/user/info">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>

          <el-menu-item index="/user/setting">
            <el-icon><Setting /></el-icon>
            <span>账号设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  House, Tickets, User, Setting,
  Dish, Brush, FirstAidKit, ShoppingCart
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const userName = ref(userStore.username || '用户')

onMounted(() => {
  userStore.getUserInfo()
  userName.value = userStore.username
})

const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  background: linear-gradient(90deg, #00a88d 0%, #00c4a0 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 32px;
  font-size: 26px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(0, 184, 153, 0.25);
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.header-right {
  font-size: 19px;
  font-weight: 500;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.aside-menu {
  border-right: none;
  height: 100%;
  padding-top: 20px;
}

:deep(.el-menu-item) {
  height: 64px;
  line-height: 64px;
  font-size: 19px;
  margin: 6px 16px;
  border-radius: 14px;
  transition: all 0.25s ease;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #e6f7f3 0%, #f0fffc 100%);
  color: #008c74;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 184, 153, 0.1);
}

:deep(.el-menu-item:hover) {
  background-color: #f5fcfb;
  transform: translateX(2px);
}

.layout-main {
  background-color: #f8faf9;
  padding: 32px;
  overflow-y: auto;
}
</style>
