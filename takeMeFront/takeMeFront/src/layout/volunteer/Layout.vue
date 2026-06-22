<!-- src/layout/volunteer/Layout.vue 全量替换 -->
<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">takeMe志愿者服务平台</div>
      <div class="header-right">
        <div class="user-box">
          <Avatar :src="volunteerStore.avatar" :size="46" />
          <span>您好，{{ userName }} 志愿者</span>
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
          <el-menu-item index="/volunteer">
            <el-icon><House /></el-icon>
            <span>志愿首页</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/getTodo">
            <el-icon><List /></el-icon>
            <span>接取服务</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/todo">
            <el-icon><Clock /></el-icon>
            <span>我的待办</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/record">
            <el-icon><DocumentChecked /></el-icon>
            <span>服务记录</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/points">
            <el-icon><Coin /></el-icon>
            <span>积分薪酬</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/leave">
            <el-icon><Calendar /></el-icon>
            <span>出勤请假</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/study">
            <el-icon><Reading /></el-icon>
            <span>学习规范</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/info">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/message">
            <el-icon><ChatDotRound /></el-icon>
            <span>消息中心</span>
          </el-menu-item>

          <el-menu-item index="/volunteer/setting">
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
import { useVolunteerStore } from '@/stores/volunteer'
import Avatar from '@/components/Avatar.vue'
import {
  House, User, Setting, Clock, Calendar,
  ChatDotRound, Coin, Reading, List, DocumentChecked
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const volunteerStore = useVolunteerStore()

const activeMenu = computed(() => route.path)
const userName = ref(volunteerStore.realName || '志愿者')

onMounted(async () => {
  await volunteerStore.fetchVolunteerInfo()
  userName.value = volunteerStore.realName || '志愿者'
})

const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  flex-shrink: 0;
  z-index: 1000;
  width: 100%;
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

.layout-container > .el-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  width: 100%;
}

.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
  flex-shrink: 0;
  overflow-y: auto;
  overflow-x: hidden;
  height: 100%;
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
  padding: 0;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  height: 100%;
}
</style>
