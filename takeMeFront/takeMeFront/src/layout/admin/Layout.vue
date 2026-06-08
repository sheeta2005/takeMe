<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">takeMe 管理员后台</div>
      <div class="header-right">
        <div class="user-box">
          <span>您好，{{ adminName }} 管理员</span>
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
          <el-menu-item index="/admin/index">
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
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/approval">
            <el-icon><Check /></el-icon>
            <span>业务审批</span>
          </el-menu-item>
          <el-menu-item index="/admin/servicePackage">
            <el-icon><Goods /></el-icon>
            <span>服务套餐管理</span>
          </el-menu-item>

          <el-menu-item index="/admin/sendMsg">
            <el-icon><ChatDotRound /></el-icon>
            <span>发送消息</span>
          </el-menu-item>
          <el-menu-item index="/admin/message">
            <el-icon><Message /></el-icon>
            <span>消息中心</span>
          </el-menu-item>

          <el-menu-item index="/admin/setting" class="setting-menu-item">
            <el-icon><Setting /></el-icon>
            <span>设置</span>
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
import { useAdminStore } from '@/stores/admin'
import {
  House, Document, User, Check, ChatDotRound, Message, Goods, Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const adminName = ref('管理员')

onMounted(() => {
  adminStore.fetchAdminInfo()
  adminName.value = adminStore.realName || '管理员'
})

const activeMenu = computed(() => route.path)

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
  box-shadow: 0 2px 12px rgba(0, 184, 141, 0.25);
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
.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e0e7eb;
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
  box-shadow: 0 2px 8px rgba(0, 184, 141, 0.1);
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
