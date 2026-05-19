<template>
  <el-container class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-left">takeMe 老人服务平台</div>
      <div class="header-right">
        <span>您好，{{ userName }} 老人</span>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧导航栏 -->
      <el-aside width="220px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          class="aside-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/user">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <!-- 助餐服务 -->
          <el-menu-item index="/user/meal">
            <el-icon><Dish /></el-icon>
            <span>助餐服务</span>
          </el-menu-item>

          <!-- 助洁服务 -->
          <el-menu-item index="/user/clean">
            <el-icon><Brush /></el-icon>
            <span>助洁服务</span>
          </el-menu-item>

          <!-- 助医服务 -->
          <el-menu-item index="/user/medical">
            <el-icon><FirstAidKit /></el-icon>
            <span>助医服务</span>
          </el-menu-item>

          <!-- 代购服务 -->
          <el-menu-item index="/user/shop">
            <el-icon><ShoppingCart /></el-icon>
            <span>代购服务</span>
          </el-menu-item>


          <el-menu-item index="/user/order">
            <el-icon><Tickets /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/user/info/edit">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/user/setting">
            <el-icon><Setting /></el-icon>
            <span>账号设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区：路由出口 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  House, Tickets, User, Setting,
  Dish, Brush, FirstAidKit, ShoppingCart
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前激活的菜单，和路由地址绑定
const activeMenu = computed(() => route.path)
const userName = ref(userStore.username || '用户')

// 菜单点击，直接跳转到对应路由
const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 顶部栏（深青绿色，和左侧区分） */
.layout-header {
  background-color: #00b899;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  font-size: 20px;
  font-weight: 500;
}
.header-right {
  font-size: 18px;
}

/* 左侧导航栏（白色背景） */
.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
}
.aside-menu {
  border-right: none;
  height: 100%;
  padding-top: 12px;
}
:deep(.el-menu-item) {
  height: 60px;
  line-height: 60px;
  font-size: 18px;
  margin: 4px 12px;
  border-radius: 12px;
}
/* 激活状态：浅青绿色，和顶部深绿色区分 */
:deep(.el-menu-item.is-active) {
  background-color: #e6f7f3;
  color: #00b899;
  font-weight: bold;
}
:deep(.el-menu-item:hover) {
  background-color: #f2faf8;
}

/* 主内容区 */
.layout-main {
  background-color: #f8faf9;
  padding: 24px;
  overflow-y: auto;
}
</style>
