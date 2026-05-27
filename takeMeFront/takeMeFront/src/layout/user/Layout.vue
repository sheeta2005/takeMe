<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">takeMe 老人服务平台</div>
      <div class="header-right">
        <div class="user-box">
          <img class="user-avatar" :src="userStore.avatar" alt="头像" />
          <span>您好，{{ userName }} 用户</span>

          <!-- 消息中心入口（带红点提醒）-->
          <div
            class="msg-entry"
            @click="goToMessage"
          >
            <el-icon size="20"><Bell /></el-icon>
            <div class="unread-dot" v-if="hasUnread"></div>
          </div>
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

          <!-- ✅ 新增：陪伴服务 -->
          <el-menu-item index="/user/companion">
            <el-icon><Service /></el-icon>
            <span>陪伴服务</span>
          </el-menu-item>

          <el-menu-item index="/user/order">
            <el-icon><Tickets /></el-icon>
            <span>我的订单</span>
          </el-menu-item>

          <!-- 新增：消息中心菜单 -->
          <el-menu-item index="/user/message">
            <el-icon><Bell /></el-icon>
            <span>消息中心</span>
            <div class="unread-dot" v-if="hasUnread"></div>
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
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  House, Tickets, User, Setting, Bell,
  Dish, Brush, FirstAidKit, ShoppingCart,
  Service // ✅ 新增陪伴服务图标
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前激活菜单
const activeMenu = computed(() => route.path)
const userName = ref(userStore.username || '用户')

// 未读消息红点（模拟）
const hasUnread = ref(true)

onMounted(() => {
  userStore.getUserInfo()
  userName.value = userStore.username
})

// 菜单跳转
const handleMenuSelect = (path: string) => {
  router.push(path)
}

// 右上角跳消息中心
const goToMessage = () => {
  router.push('/user/message')
}
</script>

<style scoped>
/* 页面切换动画 */
:deep(.fade-enter-active) {
  transition: opacity 0.25s ease;
}
:deep(.fade-leave-active) {
  transition: opacity 0.2s ease;
}
:deep(.fade-enter-from),
:deep(.fade-leave-to) {
  opacity: 0;
}

.layout-container {
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.layout-header {
  background: linear-gradient(90deg, #00a88d 0%, #00c4a0 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  height: 70px;
  font-size: 24px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(0, 184, 153, 0.25);
  border-bottom: 1px solid rgba(255,255,255,0.1);
  position: relative;
  z-index: 10;
}

.header-right {
  font-size: 18px;
  font-weight: 500;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 消息入口图标 */
.msg-entry {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  cursor: pointer;
  transition: 0.2s;
}
.msg-entry:hover {
  background: rgba(255,255,255,0.25);
}

/* 未读红点 */
.unread-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background-color: #f56c6c;
  border-radius: 50%;
  border: 2px solid #00a88d;
}

.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.aside-menu {
  border-right: none;
  height: 100%;
  padding-top: 10px;
}

:deep(.el-menu-item) {
  height: 60px;
  line-height: 60px;
  font-size: 18px;
  margin: 4px 12px;
  border-radius: 12px;
  transition: all 0.25s ease;
  position: relative;
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
  padding: 24px 32px;
  overflow-y: auto;
  height: calc(100vh - 70px);
  box-sizing: border-box;
}
</style>
